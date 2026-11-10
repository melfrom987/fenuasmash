import { delay, http, HttpResponse } from 'msw';
import type { Event } from '@/api/models';
import { DISCIPLINES } from '@/features/events/constants';
import { db } from '../data';

type EventBody = Partial<
  Pick<
    Event,
    'name' | 'discipline' | 'categoryType' | 'categoryLabel' | 'plannedDate' | 'maxEntries'
  >
>;

const byDisplayOrder = (a: Event, b: Event) =>
  DISCIPLINES.indexOf(a.discipline) - DISCIPLINES.indexOf(b.discipline) ||
  a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' });

const error = (
  status: number,
  code: string,
  detail: string,
  errors?: { field: string; message: string }[],
) => HttpResponse.json({ code, detail, status, errors }, { status });

const nameTaken = (tournamentId: string, name: string, exceptId?: string) =>
  db.events.some(
    (e) =>
      e.tournamentId === tournamentId &&
      e.id !== exceptId &&
      e.name.toLowerCase() === name.toLowerCase(),
  );

const missingCategoryLabel = (body: EventBody) =>
  body.categoryType !== undefined && body.categoryType !== 'OPEN' && !body.categoryLabel?.trim();

export const eventHandlers = [
  http.get('/api/tournaments/:tournamentId/events', async ({ params }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    if (!db.tournaments.some((t) => t.id === tournamentId)) {
      return error(404, 'TOURNAMENT_NOT_FOUND', 'Tournament not found');
    }
    return HttpResponse.json(
      db.events.filter((e) => e.tournamentId === tournamentId).sort(byDisplayOrder),
    );
  }),

  http.post('/api/tournaments/:tournamentId/events', async ({ params, request }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    if (!db.tournaments.some((t) => t.id === tournamentId)) {
      return error(404, 'TOURNAMENT_NOT_FOUND', 'Tournament not found');
    }
    const body = (await request.json()) as EventBody;
    const name = (body.name ?? '').trim();
    if (missingCategoryLabel(body)) {
      return error(400, 'VALIDATION_FAILED', 'Invalid request', [
        { field: 'categoryLabel', message: 'must not be blank' },
      ]);
    }
    if (nameTaken(tournamentId, name)) {
      return error(409, 'EVENT_NAME_ALREADY_EXISTS', 'Event name already exists');
    }

    const event: Event = {
      id: crypto.randomUUID(),
      tournamentId,
      name,
      discipline: body.discipline ?? 'MS',
      categoryType: body.categoryType ?? 'OPEN',
      categoryLabel: body.categoryType === 'OPEN' ? null : (body.categoryLabel ?? null),
      plannedDate: body.plannedDate ?? null,
      maxEntries: body.maxEntries ?? null,
      poolCount: null,
      poolSize: null,
      qualifiersPerPool: null,
    };
    db.events.push(event);
    return HttpResponse.json(event, { status: 201 });
  }),

  http.get('/api/events/:eventId', async ({ params }) => {
    await delay();
    const event = db.events.find((e) => e.id === params.eventId);
    return event ? HttpResponse.json(event) : error(404, 'EVENT_NOT_FOUND', 'Event not found');
  }),

  http.patch('/api/events/:eventId', async ({ params, request }) => {
    await delay();
    const event = db.events.find((e) => e.id === params.eventId);
    if (!event) return error(404, 'EVENT_NOT_FOUND', 'Event not found');

    const body = (await request.json()) as EventBody;
    const name = body.name?.trim() ?? event.name;
    if (missingCategoryLabel(body)) {
      return error(400, 'VALIDATION_FAILED', 'Invalid request', [
        { field: 'categoryLabel', message: 'must not be blank' },
      ]);
    }
    if (nameTaken(event.tournamentId, name, event.id)) {
      return error(409, 'EVENT_NAME_ALREADY_EXISTS', 'Event name already exists');
    }

    // Un champ envoyé à null est effacé ; un champ absent est conservé
    Object.assign(event, body, { name });
    if (event.categoryType === 'OPEN') event.categoryLabel = null;
    return HttpResponse.json(event);
  }),

  http.delete('/api/events/:eventId', async ({ params }) => {
    await delay();
    const index = db.events.findIndex((e) => e.id === params.eventId);
    if (index === -1) return error(404, 'EVENT_NOT_FOUND', 'Event not found');
    db.events.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),
];
