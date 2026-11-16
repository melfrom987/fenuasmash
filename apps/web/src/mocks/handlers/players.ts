import { delay, http, HttpResponse } from 'msw';
import type { Player, PlayerPage } from '@/api/models';
import type { PlayerInput } from '@/features/players/schema';
import { db } from '../data';
import { normalize, problem } from './utils';

const MAX_PAGE_SIZE = 100;

const byName = (a: Player, b: Player) =>
  a.lastName.localeCompare(b.lastName, 'fr', { sensitivity: 'base' }) ||
  a.firstName.localeCompare(b.firstName, 'fr', { sensitivity: 'base' });

const matchesSearch = (player: Player, q: string) =>
  !q ||
  normalize(`${player.firstName} ${player.lastName}`).includes(q) ||
  normalize(`${player.lastName} ${player.firstName}`).includes(q) ||
  normalize(player.licenseNumber ?? '').includes(q);

const licenseTaken = (license: string | null | undefined, exceptId?: string) =>
  !!license &&
  db.players.some(
    (p) => p.id !== exceptId && normalize(p.licenseNumber ?? '') === normalize(license),
  );

const missingFields = (body: Partial<PlayerInput>) =>
  (['firstName', 'lastName', 'gender', 'birthDate', 'clubId'] as const)
    .filter((field) => !String(body[field] ?? '').trim())
    .map((field) => ({ field, message: 'must not be blank' }));

export const playerHandlers = [
  http.get('/api/players', async ({ request }) => {
    await delay();
    const params = new URL(request.url).searchParams;
    const q = normalize(params.get('q') ?? '');
    const clubId = params.get('clubId');
    const gender = params.get('gender');
    const page = Math.max(0, Number(params.get('page') ?? 0));
    const size = Math.min(MAX_PAGE_SIZE, Math.max(1, Number(params.get('size') ?? 20)));

    const filtered = db.players
      .filter((p) => matchesSearch(p, q))
      .filter((p) => !clubId || p.club.id === clubId)
      .filter((p) => !gender || p.gender === gender)
      .sort(byName);

    const body: PlayerPage = {
      items: filtered.slice(page * size, (page + 1) * size),
      page,
      size,
      totalItems: filtered.length,
    };
    return HttpResponse.json(body);
  }),

  http.post('/api/players', async ({ request }) => {
    await delay();
    const body = (await request.json()) as Partial<PlayerInput>;
    const missing = missingFields(body);
    if (missing.length > 0) return problem(400, 'VALIDATION_FAILED', 'Invalid request', missing);

    const club = db.clubs.find((c) => c.id === body.clubId);
    if (!club) return problem(400, 'INVALID_CLUB', 'This club does not exist');
    if (licenseTaken(body.licenseNumber)) {
      return problem(409, 'PLAYER_LICENSE_ALREADY_EXISTS', 'A player already has this license');
    }

    const player: Player = {
      id: crypto.randomUUID(),
      firstName: body.firstName!.trim(),
      lastName: body.lastName!.trim(),
      gender: body.gender!,
      birthDate: body.birthDate!,
      club: { id: club.id, name: club.name, logoUrl: club.logoUrl },
      licenseNumber: body.licenseNumber ?? null,
      email: body.email ?? null,
      phone: body.phone ?? null,
      rankings: body.rankings ?? {
        singles: { points: null, position: null },
        doubles: { points: null, position: null },
        mixed: { points: null, position: null },
      },
    };
    db.players.push(player);
    return HttpResponse.json(player, { status: 201 });
  }),

  http.get('/api/players/:playerId', async ({ params }) => {
    await delay();
    const player = db.players.find((p) => p.id === params.playerId);
    return player
      ? HttpResponse.json(player)
      : problem(404, 'PLAYER_NOT_FOUND', 'Player not found');
  }),

  http.patch('/api/players/:playerId', async ({ params, request }) => {
    await delay();
    const player = db.players.find((p) => p.id === params.playerId);
    if (!player) return problem(404, 'PLAYER_NOT_FOUND', 'Player not found');

    const { clubId, ...rest } = (await request.json()) as Partial<PlayerInput>;
    if (clubId !== undefined) {
      const club = db.clubs.find((c) => c.id === clubId);
      if (!club) return problem(400, 'INVALID_CLUB', 'This club does not exist');
      player.club = { id: club.id, name: club.name, logoUrl: club.logoUrl };
    }
    if (licenseTaken(rest.licenseNumber, player.id)) {
      return problem(409, 'PLAYER_LICENSE_ALREADY_EXISTS', 'A player already has this license');
    }
    Object.assign(player, rest);
    return HttpResponse.json(player);
  }),
];
