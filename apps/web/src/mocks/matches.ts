import type { Court, Draw, Event, Match, Registration } from '@/api/models';
import { makeDraw, toDrawEntries } from '@/features/draw/algorithm';
import { generateEventMatches } from '@/features/schedule/generate';
import { advance } from './advance';

const minutesAgo = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString();

export function seededRandom(seed: number) {
  let state = seed;
  return () => {
    state = (state * 16807) % 2147483647;
    return state / 2147483647;
  };
}

export function stampMatches(
  generated: ReturnType<typeof generateEventMatches>,
  now = new Date().toISOString(),
): Match[] {
  return generated.map((match) => ({ ...match, stateVersion: 0, createdAt: now, updatedAt: now }));
}

export function nextNumber(matches: Match[], tournamentId: string) {
  return (
    Math.max(
      0,
      ...matches.filter((m) => m.tournamentId === tournamentId).map((m) => m.number ?? 0),
    ) + 1
  );
}

export function seedPlay(events: Event[], registrations: Registration[], courts: Court[]) {
  const [singles] = events;
  const confirmed = registrations.filter(
    (r) => r.eventId === singles.id && r.status === 'CONFIRMED',
  );
  const plan = makeDraw({
    format: 'POOLS_THEN_ELIMINATION',
    ranked: toDrawEntries(confirmed, singles.discipline),
    poolCount: 2,
    qualifiersPerPool: 2,
    seedCount: null,
    random: seededRandom(7),
  });
  const draw: Draw = {
    eventId: singles.id,
    status: 'PUBLISHED',
    seedCount: plan.seedCount,
    pools: plan.pools,
    bracket: plan.bracket,
    excludedRegistrationIds: [],
    createdAt: minutesAgo(600),
    publishedAt: minutesAgo(590),
  };

  let n = 0;
  const matches = stampMatches(
    generateEventMatches(
      singles,
      draw,
      1,
      () => `60000000-0000-4000-8000-${String(++n).padStart(12, '0')}`,
    ),
    minutesAgo(590),
  );

  const [first, second, third] = matches;
  Object.assign(first, {
    status: 'FINISHED',
    courtId: courts[2].id,
    winner: 'A',
    outcome: 'NORMAL',
    startedAt: minutesAgo(70),
    endedAt: minutesAgo(40),
    updatedAt: minutesAgo(40),
    stateVersion: 4,
  } satisfies Partial<Match>);
  advance(first, matches, [draw]);
  Object.assign(second, {
    status: 'IN_PROGRESS',
    courtId: courts[0].id,
    startedAt: minutesAgo(18),
    updatedAt: minutesAgo(18),
    stateVersion: 3,
  } satisfies Partial<Match>);
  Object.assign(third, {
    status: 'CALLED',
    courtId: courts[1].id,
    updatedAt: minutesAgo(3),
    stateVersion: 1,
  } satisfies Partial<Match>);

  return { draws: [draw], matches };
}
