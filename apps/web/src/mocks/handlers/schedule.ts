import { delay, http, HttpResponse } from 'msw';
import type {
  Schedule,
  ScheduleEvent,
  ScheduleGenerateRequest,
  ScheduleMoveRequest,
  ScheduleStage,
} from '@/api/models';
import { checkSchedule, PLANNABLE, planSchedule } from '@/features/schedule/planner';
import { db } from '../data';
import { problem } from './utils';

const MINUTE = 60_000;
const STAGES: ScheduleStage[] = ['KNOCKOUT', 'QUARTER', 'SEMI', 'FINAL'];

function stateOf(tournamentId: string) {
  let state = db.schedules.find((s) => s.tournamentId === tournamentId);
  if (!state) {
    state = {
      tournamentId,
      status: 'NOT_GENERATED',
      generatedAt: null,
      publishedAt: null,
      eventSettings: [],
    };
    db.schedules.push(state);
  }
  return state;
}

function inputOf(tournamentId: string) {
  const tournament = db.tournaments.find((t) => t.id === tournamentId);
  if (!tournament) return null;
  return {
    tournament,
    events: db.events.filter((e) => e.tournamentId === tournamentId),
    courts: db.courts.filter((c) => c.tournamentId === tournamentId),
    registrations: db.registrations.filter((r) => r.tournamentId === tournamentId),
    matches: db.matches.filter((m) => m.tournamentId === tournamentId),
  };
}

function scheduleOf(tournamentId: string): Schedule | null {
  const input = inputOf(tournamentId);
  if (!input) return null;
  const state = stateOf(tournamentId);
  const duration = (input.tournament.averageMatchMinutes ?? 25) * MINUTE;
  const starts = input.matches
    .map((m) => m.plannedStartAt)
    .filter((s): s is string => !!s)
    .map((s) => Date.parse(s));
  const settingsOf = new Map(state.eventSettings.map((s) => [s.eventId, s]));
  const order = new Map(state.eventSettings.map((s, index) => [s.eventId, index]));
  const scheduled = input.events.filter((e) => input.matches.some((m) => m.eventId === e.id));
  const startOf = (eventId: string) =>
    input.events.find((e) => e.id === eventId)?.plannedDate ?? input.tournament.startsAt;
  const events: ScheduleEvent[] = scheduled
    .sort(
      (a, b) =>
        (order.get(a.id) ?? Number.MAX_SAFE_INTEGER) -
          (order.get(b.id) ?? Number.MAX_SAFE_INTEGER) ||
        startOf(a.id).localeCompare(startOf(b.id)),
    )
    .map((event) => {
      const planned = input.matches
        .filter((m) => m.eventId === event.id && m.plannedStartAt)
        .map((m) => Date.parse(m.plannedStartAt ?? ''));
      return {
        eventId: event.id,
        offsetSlots: settingsOf.get(event.id)?.offsetSlots ?? 0,
        startAt: settingsOf.get(event.id)?.startAt ?? null,
        stageStarts: settingsOf.get(event.id)?.stageStarts ?? [],
        matchCount: input.matches.filter((m) => m.eventId === event.id).length,
        firstStartAt: planned.length ? new Date(Math.min(...planned)).toISOString() : null,
        lastEndAt: planned.length ? new Date(Math.max(...planned) + duration).toISOString() : null,
      };
    });
  return {
    tournamentId,
    status: state.status,
    generatedAt: state.generatedAt,
    publishedAt: state.publishedAt,
    events,
    firstStartAt: starts.length ? new Date(Math.min(...starts)).toISOString() : null,
    lastEndAt: starts.length ? new Date(Math.max(...starts) + duration).toISOString() : null,
    warnings: state.status === 'NOT_GENERATED' ? [] : checkSchedule(input),
  };
}

const notFound = () => problem(404, 'TOURNAMENT_NOT_FOUND', 'Tournament not found');

export const scheduleHandlers = [
  http.get('/api/tournaments/:tournamentId/schedule', async ({ params }) => {
    await delay();
    const schedule = scheduleOf(String(params.tournamentId));
    return schedule ? HttpResponse.json(schedule) : notFound();
  }),

  http.post('/api/tournaments/:tournamentId/schedule', async ({ params, request }) => {
    await delay(600);
    const tournamentId = String(params.tournamentId);
    const input = inputOf(tournamentId);
    if (!input) return notFound();
    const body = (await request.json().catch(() => null)) as ScheduleGenerateRequest | null;
    if (body?.events) {
      const known = new Set(input.events.map((e) => e.id));
      const validDate = (value: unknown) =>
        typeof value === 'string' && !Number.isNaN(Date.parse(value));
      const valid = body.events.every(
        (s) =>
          known.has(s.eventId) &&
          Number.isInteger(s.offsetSlots) &&
          Math.abs(s.offsetSlots) <= 100 &&
          (s.startAt == null || validDate(s.startAt)) &&
          (s.stageStarts ?? []).every((rule) => STAGES.includes(rule.stage) && validDate(rule.at)),
      );
      if (!valid) return problem(400, 'VALIDATION_FAILED', 'Invalid event settings');
      stateOf(tournamentId).eventSettings = body.events;
    }
    const plannable = input.matches.filter((m) => PLANNABLE.includes(m.status));
    if (plannable.length === 0) {
      return problem(409, 'NO_MATCHES_TO_SCHEDULE', 'Publish a draw first');
    }
    if (!input.courts.some((c) => c.active)) {
      return problem(409, 'NO_ACTIVE_COURT', 'Enable at least one court');
    }
    for (const match of plannable) {
      match.plannedStartAt = null;
      match.plannedCourtId = null;
    }
    for (const placement of planSchedule({
      ...input,
      eventSettings: stateOf(tournamentId).eventSettings,
    })) {
      const match = db.matches.find((m) => m.id === placement.matchId);
      if (match) Object.assign(match, placement);
    }
    Object.assign(stateOf(tournamentId), {
      status: 'DRAFT',
      generatedAt: new Date().toISOString(),
      publishedAt: null,
    });
    return HttpResponse.json(scheduleOf(tournamentId));
  }),

  http.post('/api/tournaments/:tournamentId/schedule/move', async ({ params, request }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    if (!inputOf(tournamentId)) return notFound();
    if (stateOf(tournamentId).status === 'NOT_GENERATED') {
      return problem(409, 'SCHEDULE_NOT_GENERATED', 'Generate the schedule first');
    }
    const { matchId, plannedStartAt, courtId } = (await request.json()) as ScheduleMoveRequest;
    const match = db.matches.find((m) => m.id === matchId && m.tournamentId === tournamentId);
    if (!match) return problem(404, 'MATCH_NOT_FOUND', 'Match not found');
    if (!PLANNABLE.includes(match.status)) {
      return problem(409, 'MATCH_ALREADY_STARTED', 'The match is already on a court or over');
    }
    const court = db.courts.find((c) => c.id === courtId && c.tournamentId === tournamentId);
    if (!court) return problem(404, 'COURT_NOT_FOUND', 'Court not found');
    if (!court.active) return problem(409, 'COURT_INACTIVE', 'The court is disabled');

    const start = new Date(plannedStartAt).toISOString();
    const occupant = db.matches.find(
      (m) =>
        m.id !== match.id &&
        m.tournamentId === tournamentId &&
        m.plannedCourtId === court.id &&
        m.plannedStartAt === start,
    );
    if (occupant && !PLANNABLE.includes(occupant.status)) {
      return problem(409, 'SLOT_TAKEN', 'A started match uses this slot');
    }
    if (occupant) {
      occupant.plannedStartAt = match.plannedStartAt ?? null;
      occupant.plannedCourtId = match.plannedCourtId ?? null;
    }
    match.plannedStartAt = start;
    match.plannedCourtId = court.id;
    return HttpResponse.json(scheduleOf(tournamentId));
  }),

  http.post('/api/tournaments/:tournamentId/schedule/publish', async ({ params }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    if (!inputOf(tournamentId)) return notFound();
    const state = stateOf(tournamentId);
    if (state.status === 'NOT_GENERATED') {
      return problem(409, 'SCHEDULE_NOT_GENERATED', 'Generate the schedule first');
    }
    Object.assign(state, { status: 'PUBLISHED', publishedAt: new Date().toISOString() });
    return HttpResponse.json(scheduleOf(tournamentId));
  }),
];
