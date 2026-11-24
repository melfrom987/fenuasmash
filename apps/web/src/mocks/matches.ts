import type { Court, Event, Match, MatchStatus, Registration } from '@/api/models';

const minutesAgo = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString();

export function seedMatches(events: Event[], registrations: Registration[], courts: Court[]) {
  const confirmed = (eventId: string) =>
    registrations.filter((r) => r.eventId === eventId && r.status === 'CONFIRMED').map((r) => r.id);
  const [singles, , mixed] = events;
  const s = confirmed(singles.id);
  const x = confirmed(mixed.id);
  let n = 0;

  const match = (
    event: Event,
    round: number,
    sideA: string | null,
    sideB: string | null,
    status: MatchStatus,
    extra: Partial<Match> = {},
  ): Match => {
    n += 1;
    return {
      id: `60000000-0000-4000-8000-${String(n).padStart(12, '0')}`,
      tournamentId: event.tournamentId,
      eventId: event.id,
      round,
      sideA,
      sideB,
      courtId: null,
      status,
      winner: null,
      outcome: null,
      stateVersion: 1,
      startedAt: null,
      endedAt: null,
      createdAt: minutesAgo(180 - n),
      updatedAt: minutesAgo(30),
      ...extra,
    };
  };

  return [
    match(singles, 1, s[3], s[5], 'FINISHED', {
      courtId: courts[2].id,
      winner: 'A',
      outcome: 'NORMAL',
      startedAt: minutesAgo(70),
      endedAt: minutesAgo(40),
      updatedAt: minutesAgo(40),
    }),
    match(singles, 1, s[0], s[1], 'IN_PROGRESS', {
      courtId: courts[0].id,
      startedAt: minutesAgo(18),
      updatedAt: minutesAgo(18),
    }),
    match(singles, 1, s[2], s[3], 'CALLED', { courtId: courts[1].id, updatedAt: minutesAgo(3) }),
    match(singles, 1, s[4], s[5], 'READY'),
    match(singles, 1, s[0], s[2], 'READY'),
    match(singles, 1, s[1], s[4], 'READY'),
    match(mixed, 1, x[0], x[1], 'READY'),
    match(mixed, 2, x[2], null, 'SCHEDULED'),
  ];
}
