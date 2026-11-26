import { delay, http, HttpResponse } from 'msw';
import type {
  ChangeMatchStatusRequest,
  CreateMatchRequest,
  Match,
  MatchStatus,
} from '@/api/models';
import { canMove, NEEDS_BOTH_SIDES, ON_COURT } from '@/features/matches/status';
import { advance } from '../advance';
import { db } from '../data';
import { problem } from './utils';

const byRoundThenCreation = (a: Match, b: Match) =>
  (a.round ?? Number.MAX_SAFE_INTEGER) - (b.round ?? Number.MAX_SAFE_INTEGER) ||
  a.createdAt.localeCompare(b.createdAt);

const byPlannedStart = (a: Match, b: Match) =>
  (a.plannedStartAt ?? '\uffff').localeCompare(b.plannedStartAt ?? '\uffff') ||
  (a.number ?? 0) - (b.number ?? 0);

const matchesOf = (tournamentId: string) =>
  db.matches.filter((m) => m.tournamentId === tournamentId).sort(byRoundThenCreation);

const onCourt = (tournamentId: string) =>
  db.matches.filter((m) => m.tournamentId === tournamentId && ON_COURT.includes(m.status));

const sides = (match: Match) => [match.sideA, match.sideB].filter((s): s is string => s !== null);

const sideOnAnotherCourt = (match: Match, courtId: string) =>
  onCourt(match.tournamentId).some(
    (other) =>
      other.id !== match.id &&
      other.courtId !== courtId &&
      sides(other).some((side) => sides(match).includes(side)),
  );

const courtTaken = (match: Match, courtId: string) =>
  onCourt(match.tournamentId).some((other) => other.id !== match.id && other.courtId === courtId);

const touch = (match: Match) => {
  match.stateVersion += 1;
  match.updatedAt = new Date().toISOString();
  return HttpResponse.json(match);
};

const findMatch = (id: unknown) => db.matches.find((m) => m.id === id);
const notFound = () => problem(404, 'MATCH_NOT_FOUND', 'Match not found');
const versionConflict = () =>
  problem(409, 'STATE_VERSION_CONFLICT', 'The match changed since it was read');

export const matchHandlers = [
  http.get('/api/tournaments/:tournamentId/matches', async ({ params, request }) => {
    await delay();
    const statuses = new URL(request.url).searchParams.getAll('status');
    const matches = matchesOf(String(params.tournamentId));
    return HttpResponse.json(
      statuses.length ? matches.filter((m) => statuses.includes(m.status)) : matches,
    );
  }),

  http.get('/api/events/:eventId/matches', async ({ params }) => {
    await delay();
    return HttpResponse.json(
      db.matches.filter((m) => m.eventId === params.eventId).sort(byRoundThenCreation),
    );
  }),

  http.post('/api/events/:eventId/matches', async ({ params, request }) => {
    await delay();
    const event = db.events.find((e) => e.id === params.eventId);
    if (!event) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');
    const {
      round = null,
      sideA = null,
      sideB = null,
    } = (await request.json()) as Partial<CreateMatchRequest>;
    if ((round !== null && (round < 1 || round > 20)) || (sideA !== null && sideA === sideB)) {
      return problem(400, 'VALIDATION_FAILED', 'Invalid match');
    }
    const now = new Date().toISOString();
    const match: Match = {
      id: crypto.randomUUID(),
      tournamentId: event.tournamentId,
      eventId: event.id,
      round,
      sideA,
      sideB,
      courtId: null,
      status: 'SCHEDULED',
      winner: null,
      outcome: null,
      stateVersion: 0,
      startedAt: null,
      endedAt: null,
      createdAt: now,
      updatedAt: now,
    };
    db.matches.push(match);
    return HttpResponse.json(match, { status: 201 });
  }),

  http.get('/api/tournaments/:tournamentId/match-queue', async ({ params }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    const busy = new Set(onCourt(tournamentId).flatMap(sides));
    return HttpResponse.json(
      matchesOf(tournamentId)
        .filter((m) => m.status === 'READY')
        .sort(byPlannedStart)
        .map((match) => ({ match, available: !sides(match).some((side) => busy.has(side)) })),
    );
  }),

  http.get('/api/tournaments/:tournamentId/court-assignments', async ({ params }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    const playing = onCourt(tournamentId);
    return HttpResponse.json(
      db.courts
        .filter((c) => c.tournamentId === tournamentId)
        .sort((a, b) => a.number - b.number)
        .map((court) => ({ court, match: playing.find((m) => m.courtId === court.id) ?? null })),
    );
  }),

  http.get('/api/matches/:matchId', async ({ params }) => {
    await delay();
    const match = findMatch(params.matchId);
    return match ? HttpResponse.json(match) : notFound();
  }),

  http.post('/api/matches/:matchId/court', async ({ params, request }) => {
    await delay();
    const match = findMatch(params.matchId);
    if (!match) return notFound();
    const { courtId, stateVersion } = (await request.json()) as {
      courtId: string;
      stateVersion: number;
    };
    const court = db.courts.find((c) => c.id === courtId);
    if (!court) return problem(404, 'COURT_NOT_FOUND', 'Court not found');
    if (court.tournamentId !== match.tournamentId) {
      return problem(400, 'INVALID_COURT', 'The court is not in the tournament of the match');
    }
    if (stateVersion !== match.stateVersion) return versionConflict();
    if (match.status !== 'READY' && match.status !== 'CALLED') {
      return problem(409, 'INVALID_STATUS_TRANSITION', 'Only a READY or CALLED match');
    }
    if (!court.active) return problem(409, 'COURT_INACTIVE', 'The court is disabled');
    if (courtTaken(match, court.id)) return problem(409, 'COURT_OCCUPIED', 'Court occupied');
    if (sideOnAnotherCourt(match, court.id)) {
      return problem(409, 'SIDE_ON_ANOTHER_COURT', 'A side is on another court');
    }
    match.courtId = court.id;
    match.status = 'CALLED';
    return touch(match);
  }),

  http.post('/api/matches/:matchId/status', async ({ params, request }) => {
    await delay();
    const match = findMatch(params.matchId);
    if (!match) return notFound();
    const {
      status,
      stateVersion,
      winner = null,
      outcome = null,
    } = (await request.json()) as ChangeMatchStatusRequest;
    if (stateVersion !== match.stateVersion) return versionConflict();
    if (status === 'CALLED' && match.status === 'READY') {
      return problem(409, 'COURT_REQUIRED', 'Call a match with POST /matches/{id}/court');
    }
    if (!canMove(match.status, status)) {
      return problem(409, 'INVALID_STATUS_TRANSITION', `${match.status} → ${status}`);
    }
    if (NEEDS_BOTH_SIDES(status) && (!match.sideA || !match.sideB)) {
      return problem(409, 'MATCH_SIDES_INCOMPLETE', 'Both sides are needed');
    }
    const needsWinner = status === 'FINISHED' || status === 'WALKOVER';
    if (needsWinner !== (winner !== null) || (status === 'FINISHED') !== (outcome !== null)) {
      return problem(400, 'INVALID_MATCH_RESULT', 'Invalid result for this status');
    }
    if (match.status === 'FINISHED' && match.courtId) {
      if (courtTaken(match, match.courtId)) return problem(409, 'COURT_OCCUPIED', 'Court occupied');
      if (sideOnAnotherCourt(match, match.courtId)) {
        return problem(409, 'SIDE_ON_ANOTHER_COURT', 'A side is on another court');
      }
    }

    const now = new Date().toISOString();
    const ending: MatchStatus[] = ['FINISHED', 'CANCELLED', 'WALKOVER'];
    match.status = status;
    match.winner = winner;
    match.outcome = status === 'WALKOVER' ? 'WALKOVER' : outcome;
    if (status === 'READY' || status === 'SCHEDULED') match.courtId = null;
    if (status === 'IN_PROGRESS') match.startedAt ??= now;
    match.endedAt = ending.includes(status) ? now : null;
    advance(match, db.matches, db.draws);
    return touch(match);
  }),
];
