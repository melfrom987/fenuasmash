import { delay, http, HttpResponse } from 'msw';
import type { Event, EventFormat, EventType, ScoringRules } from '@/api/models';
import { DEFAULT_SCORING, EVENT_TYPES } from '@/features/events/constants';
import { db } from '../data';

type EventBody = Partial<Omit<Event, 'id' | 'tournamentId' | 'scoring'>> & {
  scoring?: Partial<ScoringRules>;
};

const byDisplayOrder = (a: Event, b: Event) =>
  EVENT_TYPES.indexOf(a.type as (typeof EVENT_TYPES)[number]) -
    EVENT_TYPES.indexOf(b.type as (typeof EVENT_TYPES)[number]) ||
  a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' });

const error = (status: number, code: string, detail: string) =>
  HttpResponse.json({ code, detail, status }, { status });

const nameTaken = (tournamentId: string, name: string, exceptId?: string) =>
  db.events.some(
    (e) =>
      e.tournamentId === tournamentId &&
      e.id !== exceptId &&
      e.name.toLowerCase() === name.toLowerCase(),
  );

function resolveSettings(
  current: Pick<Event, 'format' | 'poolSize' | 'qualifiersPerPool' | 'scoring'>,
  body: EventBody,
) {
  const format: EventFormat = body.format ?? current.format;
  let poolSize: number | null = null;
  let qualifiersPerPool: number | null = null;

  if (format === 'POOLS_THEN_ELIMINATION') {
    const keep = current.format === 'POOLS_THEN_ELIMINATION';
    poolSize = body.poolSize ?? (keep ? current.poolSize : null);
    qualifiersPerPool = body.qualifiersPerPool ?? (keep ? current.qualifiersPerPool : null);
    if (poolSize == null || qualifiersPerPool == null || qualifiersPerPool >= poolSize) {
      return error(400, 'INVALID_FORMAT_SETTINGS', 'Invalid pool settings');
    }
  } else if (body.poolSize != null || body.qualifiersPerPool != null) {
    return error(400, 'INVALID_FORMAT_SETTINGS', 'Pool settings are only allowed for pools');
  }

  const scoring = { ...current.scoring, ...body.scoring };
  if (scoring.pointCap < scoring.pointsToWin) {
    return error(400, 'INVALID_SCORING_RULES', 'pointCap must be >= pointsToWin');
  }
  return { format, poolSize, qualifiersPerPool, scoring };
}

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
    if (nameTaken(tournamentId, name)) {
      return error(409, 'EVENT_NAME_ALREADY_EXISTS', 'Event name already exists');
    }
    const settings = resolveSettings(
      {
        format: 'SINGLE_ELIMINATION',
        poolSize: null,
        qualifiersPerPool: null,
        scoring: DEFAULT_SCORING,
      },
      body,
    );
    if (settings instanceof Response) return settings;

    const event: Event = {
      id: crypto.randomUUID(),
      tournamentId,
      name,
      type: body.type as EventType,
      ...settings,
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
    if (nameTaken(event.tournamentId, name, event.id)) {
      return error(409, 'EVENT_NAME_ALREADY_EXISTS', 'Event name already exists');
    }
    const settings = resolveSettings(event, body);
    if (settings instanceof Response) return settings;

    Object.assign(event, { name, type: body.type ?? event.type, ...settings });
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
