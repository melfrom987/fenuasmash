import type { Draw, Event, Match, MatchPhase, MatchSource, Pool } from '@/api/models';

type Feeder =
  | { kind: 'ENTRY'; registrationId: string }
  | { kind: 'SOURCE'; source: MatchSource }
  | { kind: 'BYE' };

export type GeneratedMatch = Omit<
  Match,
  'id' | 'createdAt' | 'updatedAt' | 'stateVersion' | 'number'
> & { id: string; number: number };

export function roundRobin<T>(entries: T[]): [T, T][][] {
  const players: (T | null)[] = entries.length % 2 === 0 ? [...entries] : [...entries, null];
  const rounds: [T, T][][] = [];
  for (let round = 0; round < players.length - 1; round++) {
    const pairs: [T, T][] = [];
    for (let i = 0; i < players.length / 2; i++) {
      const a = players[i];
      const b = players[players.length - 1 - i];
      if (a !== null && b !== null) pairs.push(round % 2 === 0 ? [a, b] : [b, a]);
    }
    rounds.push(pairs);
    players.splice(1, 0, players.pop() ?? null);
  }
  return rounds;
}

type PoolSide = number | { winner: string } | { loser: string };
type PoolMatchSpec = [PoolSide, PoolSide];

export const POOL_ORDERS: Record<number, PoolMatchSpec[][]> = {
  3: [[[1, 2]], [[0, { loser: '0.0' }]], [[0, { winner: '0.0' }]]],
  4: [
    [
      [0, 2],
      [1, 3],
    ],
    [
      [{ winner: '0.0' }, { loser: '0.1' }],
      [{ winner: '0.1' }, { loser: '0.0' }],
    ],
    [
      [{ winner: '0.0' }, { winner: '0.1' }],
      [{ loser: '0.0' }, { loser: '0.1' }],
    ],
  ],
  5: [
    [
      [1, 3],
      [2, 4],
    ],
    [
      [0, 2],
      [3, 4],
    ],
    [
      [0, 3],
      [1, 4],
    ],
    [
      [0, 4],
      [1, 2],
    ],
    [
      [0, 1],
      [2, 3],
    ],
  ],
};

export function poolOrder(size: number): PoolMatchSpec[][] {
  const fixed = POOL_ORDERS[size];
  if (fixed) return fixed;
  const indexes = Array.from({ length: size }, (_, i) => i);
  return roundRobin(indexes).map((round) => round.map(([a, b]) => [a, b] as PoolMatchSpec));
}

export function generateEventMatches(
  event: Event,
  draw: Draw,
  firstNumber: number,
  newId: () => string,
): GeneratedMatch[] {
  const matches: GeneratedMatch[] = [];

  const add = (
    phase: MatchPhase,
    round: number,
    pool: string | null,
    a: Feeder,
    b: Feeder,
  ): GeneratedMatch => {
    const match: GeneratedMatch = {
      id: newId(),
      number: firstNumber + matches.length,
      tournamentId: event.tournamentId,
      eventId: event.id,
      round,
      phase,
      pool,
      sideA: a.kind === 'ENTRY' ? a.registrationId : null,
      sideB: b.kind === 'ENTRY' ? b.registrationId : null,
      sourceA: a.kind === 'SOURCE' ? a.source : null,
      sourceB: b.kind === 'SOURCE' ? b.source : null,
      courtId: null,
      status: a.kind === 'ENTRY' && b.kind === 'ENTRY' ? 'READY' : 'SCHEDULED',
      winner: null,
      outcome: null,
      startedAt: null,
      endedAt: null,
      plannedStartAt: null,
      plannedCourtId: null,
    };
    matches.push(match);
    return match;
  };

  const poolRounds = draw.pools.map((pool: Pool) => poolOrder(pool.entries.length));
  const played = draw.pools.map(() => new Map<string, string>());
  const roundCount = Math.max(0, ...poolRounds.map((rounds) => rounds.length));
  for (let round = 0; round < roundCount; round++) {
    draw.pools.forEach((pool, p) => {
      (poolRounds[p][round] ?? []).forEach((spec, index) => {
        const [a, b] = spec.map((side): Feeder => {
          if (typeof side === 'number') {
            return { kind: 'ENTRY', registrationId: pool.entries[side].registrationId };
          }
          const winner = 'winner' in side;
          const matchId = played[p].get(winner ? side.winner : side.loser) ?? '';
          return { kind: 'SOURCE', source: { kind: winner ? 'WINNER' : 'LOSER', matchId } };
        });
        const match = add('POOL', round + 1, pool.name, a, b);
        played[p].set(`${round}.${index}`, match.id);
      });
    });
  }

  if (!draw.bracket) return matches;

  let feeders: Feeder[] = draw.bracket.slots.map((slot) =>
    slot.bye
      ? { kind: 'BYE' }
      : slot.entry
        ? { kind: 'ENTRY', registrationId: slot.entry.registrationId }
        : slot.qualifier
          ? {
              kind: 'SOURCE',
              source: { kind: 'POOL_RANK', pool: slot.qualifier.pool, rank: slot.qualifier.rank },
            }
          : { kind: 'BYE' },
  );

  let round = 1;
  let semiFinals: GeneratedMatch[] = [];
  while (feeders.length > 1) {
    const next: Feeder[] = [];
    const played: GeneratedMatch[] = [];
    for (let i = 0; i < feeders.length; i += 2) {
      const [a, b] = [feeders[i], feeders[i + 1]];
      if (a.kind === 'BYE' || b.kind === 'BYE') {
        next.push(a.kind === 'BYE' ? b : a);
        continue;
      }
      const match = add('KNOCKOUT', round, null, a, b);
      played.push(match);
      next.push({ kind: 'SOURCE', source: { kind: 'WINNER', matchId: match.id } });
    }
    if (feeders.length === 4) semiFinals = played;
    feeders = next;
    round += 1;
  }

  if (event.thirdPlaceMatch && semiFinals.length === 2) {
    add(
      'THIRD_PLACE',
      round - 1,
      null,
      { kind: 'SOURCE', source: { kind: 'LOSER', matchId: semiFinals[0].id } },
      { kind: 'SOURCE', source: { kind: 'LOSER', matchId: semiFinals[1].id } },
    );
  }

  return matches;
}

export function knockoutRoundCount(matches: Pick<Match, 'phase' | 'round'>[]) {
  return Math.max(0, ...matches.filter((m) => m.phase === 'KNOCKOUT').map((m) => m.round ?? 0));
}
