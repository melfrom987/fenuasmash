import { delay, http, HttpResponse } from 'msw';
import type { Event, Player, PlayerPage, Registration, RegistrationStatus } from '@/api/models';
import { DISCIPLINES } from '@/features/events/constants';
import { birthYearFits } from '@/features/players/age-category';
import { isDoubles, requiredGender } from '@/features/registrations/constants';
import { db } from '../data';
import { toPlayerSummary } from '../registrations';
import { isDrawPublished } from './draws';
import { normalize, problem } from './utils';

type CreateBody = { playerId?: string; partnerId?: string | null };
type UpdateBody = { partnerId?: string | null; confirm?: boolean };

const findPlayer = (id: unknown) => db.players.find((p) => p.id === id);
const findEvent = (id: unknown) => db.events.find((e) => e.id === id);

const registeredIn = (eventId: string, playerId: string, exceptId?: string) =>
  db.registrations.some(
    (r) =>
      r.eventId === eventId &&
      r.id !== exceptId &&
      (r.player.id === playerId || r.partner?.id === playerId),
  );

const canPlay = (event: Event, player: Player, partnerOf?: Player) => {
  const gender = requiredGender(event.discipline, partnerOf?.gender);
  return (gender === null || player.gender === gender) && birthYearFits(event, player.birthDate);
};

const forbiddenCombination = (event: Event, playerId: string) => {
  const tournament = db.tournaments.find((t) => t.id === event.tournamentId);
  const forbidden = (tournament?.forbiddenCombinations ?? [])
    .filter(([a, b]) => a === event.discipline || b === event.discipline)
    .map(([a, b]) => (a === event.discipline ? b : a));
  return db.registrations.some((r) => {
    const other = findEvent(r.eventId);
    return (
      other !== undefined &&
      other.id !== event.id &&
      (r.player.id === playerId || r.partner?.id === playerId) &&
      forbidden.includes(other.discipline)
    );
  });
};

const locked = () =>
  problem(409, 'DRAW_PUBLISHED', 'The draw of this event is published: registrations are locked');

const eventOrder = (r: Registration) =>
  DISCIPLINES.indexOf(findEvent(r.eventId)?.discipline ?? 'MS');

function checkPartner(event: Event, player: Player, partnerId: string, exceptId?: string) {
  const partner = findPlayer(partnerId);
  if (!partner) return problem(404, 'PLAYER_NOT_FOUND', 'Partner not found');
  if (partner.id === player.id || !canPlay(event, partner, player)) {
    return problem(400, 'PARTNER_NOT_ELIGIBLE', 'Partner cannot play in this event');
  }
  if (registeredIn(event.id, partner.id, exceptId)) {
    return problem(409, 'PARTNER_ALREADY_REGISTERED', 'Partner already registered');
  }
  if (forbiddenCombination(event, partner.id)) {
    return problem(400, 'FORBIDDEN_COMBINATION', 'Forbidden combination for the partner');
  }
  return null;
}

export const registrationHandlers = [
  http.get('/api/tournaments/:tournamentId/registrations', async ({ params, request }) => {
    await delay();
    const search = new URL(request.url).searchParams;
    const eventId = search.get('eventId');
    const status = search.get('status');
    const playerId = search.get('playerId');
    return HttpResponse.json(
      db.registrations
        .filter((r) => r.tournamentId === params.tournamentId)
        .filter((r) => !eventId || r.eventId === eventId)
        .filter((r) => !status || r.status === status)
        .filter((r) => !playerId || r.player.id === playerId || r.partner?.id === playerId)
        .sort((a, b) => eventOrder(a) - eventOrder(b) || a.createdAt.localeCompare(b.createdAt)),
    );
  }),

  http.get('/api/events/:eventId/eligible-players', async ({ params, request }) => {
    await delay();
    const event = findEvent(params.eventId);
    if (!event) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');

    const search = new URL(request.url).searchParams;
    const q = normalize(search.get('q') ?? '');
    const partnerOf = findPlayer(search.get('partnerOf'));
    const page = Math.max(0, Number(search.get('page') ?? 0));
    const size = Math.min(100, Math.max(1, Number(search.get('size') ?? 20)));

    const eligible = db.players
      .filter((p) => p.id !== partnerOf?.id)
      .filter((p) => canPlay(event, p, partnerOf))
      .filter((p) => !registeredIn(event.id, p.id))
      .filter(
        (p) =>
          !q ||
          normalize(`${p.firstName} ${p.lastName}`).includes(q) ||
          normalize(`${p.lastName} ${p.firstName}`).includes(q),
      )
      .sort(
        (a, b) =>
          a.lastName.localeCompare(b.lastName, 'fr', { sensitivity: 'base' }) ||
          a.firstName.localeCompare(b.firstName, 'fr', { sensitivity: 'base' }),
      );

    const body: PlayerPage = {
      items: eligible.slice(page * size, (page + 1) * size),
      page,
      size,
      totalItems: eligible.length,
    };
    return HttpResponse.json(body);
  }),

  http.post('/api/events/:eventId/registrations', async ({ params, request }) => {
    await delay();
    const event = findEvent(params.eventId);
    if (!event) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');
    if (isDrawPublished(event.id)) return locked();

    const body = (await request.json()) as CreateBody;
    const player = findPlayer(body.playerId);
    if (!player) return problem(404, 'PLAYER_NOT_FOUND', 'Player not found');
    const doubles = isDoubles(event.discipline);
    if (!doubles && body.partnerId) {
      return problem(400, 'VALIDATION_FAILED', 'Invalid request', [
        { field: 'partnerId', message: 'must be null for singles' },
      ]);
    }
    if (!canPlay(event, player)) {
      return problem(400, 'PLAYER_NOT_ELIGIBLE', 'Player cannot play in this event');
    }
    if (registeredIn(event.id, player.id)) {
      return problem(409, 'ALREADY_REGISTERED', 'Player already registered in this event');
    }
    if (forbiddenCombination(event, player.id)) {
      return problem(400, 'FORBIDDEN_COMBINATION', 'Forbidden combination');
    }
    if (body.partnerId) {
      const partnerError = checkPartner(event, player, body.partnerId);
      if (partnerError) return partnerError;
    }
    const count = db.registrations.filter((r) => r.eventId === event.id).length;
    if (event.maxEntries !== null && count >= event.maxEntries) {
      return problem(409, 'EVENT_FULL', 'This event is full');
    }

    const partner = body.partnerId ? findPlayer(body.partnerId)! : null;
    const status: RegistrationStatus = doubles && !partner ? 'LOOKING_FOR_PARTNER' : 'CONFIRMED';
    const registration: Registration = {
      id: crypto.randomUUID(),
      tournamentId: event.tournamentId,
      eventId: event.id,
      status,
      player: toPlayerSummary(player),
      partner: partner ? toPlayerSummary(partner) : null,
      registeredAt: new Date().toISOString(),
      createdAt: new Date().toISOString(),
    };
    db.registrations.push(registration);
    return HttpResponse.json(registration, { status: 201 });
  }),

  http.post('/api/events/:eventId/registrations/transfer', async ({ params, request }) => {
    await delay();
    const target = findEvent(params.eventId);
    if (!target) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');
    if (isDrawPublished(target.id)) return locked();
    const { registrationIds = [] } = (await request.json()) as { registrationIds?: string[] };

    const moved: Registration[] = [];
    const rejected: { registrationId: string; code: string; message: string }[] = [];
    const reject = (registrationId: string, code: string, message: string) =>
      rejected.push({ registrationId, code, message });

    for (const id of registrationIds) {
      const registration = db.registrations.find((r) => r.id === id);
      if (!registration) {
        reject(id, 'REGISTRATION_NOT_FOUND', 'Registration not found');
        continue;
      }
      if (registration.eventId === target.id) {
        moved.push(registration);
        continue;
      }
      if (isDrawPublished(registration.eventId)) {
        reject(id, 'DRAW_PUBLISHED', 'The draw of the source event is published');
        continue;
      }
      const source = findEvent(registration.eventId);
      if (
        !source ||
        source.tournamentId !== target.tournamentId ||
        source.discipline !== target.discipline
      ) {
        reject(id, 'INCOMPATIBLE_EVENT', 'The target event has another discipline');
        continue;
      }
      const player = findPlayer(registration.player.id);
      const partner = registration.partner ? findPlayer(registration.partner.id) : undefined;
      if (!player || !canPlay(target, player)) {
        reject(id, 'PLAYER_NOT_ELIGIBLE', 'Player cannot play in this event');
        continue;
      }
      if (partner && !canPlay(target, partner, player)) {
        reject(id, 'PARTNER_NOT_ELIGIBLE', 'Partner cannot play in this event');
        continue;
      }
      if (registeredIn(target.id, player.id)) {
        reject(id, 'ALREADY_REGISTERED', 'Already registered in this event');
        continue;
      }
      if (partner && registeredIn(target.id, partner.id)) {
        reject(id, 'PARTNER_ALREADY_REGISTERED', 'Partner already registered');
        continue;
      }
      const count = db.registrations.filter((r) => r.eventId === target.id).length;
      if (target.maxEntries !== null && count >= target.maxEntries) {
        reject(id, 'EVENT_FULL', 'This event is full');
        continue;
      }
      registration.eventId = target.id;
      moved.push(registration);
    }
    return HttpResponse.json({ moved, rejected });
  }),

  http.patch('/api/registrations/:registrationId', async ({ params, request }) => {
    await delay();
    const registration = db.registrations.find((r) => r.id === params.registrationId);
    if (!registration) return problem(404, 'REGISTRATION_NOT_FOUND', 'Registration not found');
    const event = findEvent(registration.eventId)!;
    if (isDrawPublished(event.id)) return locked();
    const player = findPlayer(registration.player.id)!;
    const body = (await request.json()) as UpdateBody;

    if (body.confirm) {
      if (registration.status !== 'PENDING_PARTNER') {
        return problem(409, 'NOT_PENDING', 'This registration is not pending');
      }
      registration.status = 'CONFIRMED';
      return HttpResponse.json(registration);
    }

    if (body.partnerId !== undefined) {
      if (!isDoubles(event.discipline)) {
        return problem(400, 'VALIDATION_FAILED', 'Invalid request', [
          { field: 'partnerId', message: 'must be null for singles' },
        ]);
      }
      if (body.partnerId === null) {
        registration.partner = null;
        registration.status = 'LOOKING_FOR_PARTNER';
      } else {
        const partnerError = checkPartner(event, player, body.partnerId, registration.id);
        if (partnerError) return partnerError;
        registration.partner = toPlayerSummary(findPlayer(body.partnerId)!);
        registration.status = 'CONFIRMED';
      }
    }
    return HttpResponse.json(registration);
  }),

  http.delete('/api/registrations/:registrationId', async ({ params }) => {
    await delay();
    const index = db.registrations.findIndex((r) => r.id === params.registrationId);
    if (index === -1) return problem(404, 'REGISTRATION_NOT_FOUND', 'Registration not found');
    if (isDrawPublished(db.registrations[index].eventId)) return locked();
    db.registrations.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),
];
