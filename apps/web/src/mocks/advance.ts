import type { Draw, Match, MatchSide, MatchSource } from '@/api/models';

const DONE = ['FINISHED', 'WALKOVER', 'CANCELLED'];

const sideOf = (match: Match, side: MatchSide) => (side === 'A' ? match.sideA : match.sideB);

function fill(
  matches: Match[],
  eventId: string,
  isSource: (s: MatchSource) => boolean,
  id: string,
) {
  for (const match of matches) {
    if (match.eventId !== eventId) continue;
    if (match.sourceA && isSource(match.sourceA) && match.sideA === null) match.sideA = id;
    if (match.sourceB && isSource(match.sourceB) && match.sideB === null) match.sideB = id;
    if (match.status === 'SCHEDULED' && match.sideA && match.sideB) match.status = 'READY';
  }
}

export function poolStandings(draw: Draw, pool: string, matches: Match[]) {
  const entries = draw.pools.find((p) => p.name === pool)?.entries ?? [];
  const wins = new Map(entries.map((e) => [e.registrationId, 0]));
  for (const match of matches) {
    if (match.eventId !== draw.eventId || match.pool !== pool || !match.winner) continue;
    const winner = sideOf(match, match.winner);
    if (winner) wins.set(winner, (wins.get(winner) ?? 0) + 1);
  }
  return entries
    .map((entry, order) => ({ entry, order, wins: wins.get(entry.registrationId) ?? 0 }))
    .sort((a, b) => b.wins - a.wins || a.order - b.order)
    .map(({ entry, wins: count }) => ({ entry, wins: count }));
}

export function advance(match: Match, matches: Match[], draws: Draw[]) {
  if (match.winner) {
    const winner = sideOf(match, match.winner);
    const loser = sideOf(match, match.winner === 'A' ? 'B' : 'A');
    if (winner) {
      fill(matches, match.eventId, (s) => s.kind === 'WINNER' && s.matchId === match.id, winner);
    }
    if (loser) {
      fill(matches, match.eventId, (s) => s.kind === 'LOSER' && s.matchId === match.id, loser);
    }
  }

  if (match.phase !== 'POOL' || !match.pool) return;
  const poolMatches = matches.filter((m) => m.eventId === match.eventId && m.pool === match.pool);
  if (!poolMatches.every((m) => DONE.includes(m.status))) return;
  const draw = draws.find((d) => d.eventId === match.eventId);
  if (!draw) return;
  poolStandings(draw, match.pool, matches).forEach(({ entry }, index) =>
    fill(
      matches,
      match.eventId,
      (s) => s.kind === 'POOL_RANK' && s.pool === match.pool && s.rank === index + 1,
      entry.registrationId,
    ),
  );
}
