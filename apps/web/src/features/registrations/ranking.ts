import type { Discipline, PlayerRankings, Registration } from '@/api/models';

const RANKING_OF: Record<Discipline, keyof PlayerRankings> = {
  MS: 'singles',
  WS: 'singles',
  MD: 'doubles',
  WD: 'doubles',
  OD: 'doubles',
  XD: 'mixed',
};

export function entryPoints(registration: Registration, discipline: Discipline): number | null {
  const ranking = RANKING_OF[discipline];
  const points = [registration.player, registration.partner]
    .filter((p) => p !== null)
    .map((p) => p.rankings[ranking].points);
  if (points.every((p) => p === null)) return null;
  return points.reduce<number>((sum, p) => sum + (p ?? 0), 0);
}

export function sortByRanking(registrations: Registration[], discipline: Discipline) {
  return [...registrations].sort((a, b) => {
    const pa = entryPoints(a, discipline);
    const pb = entryPoints(b, discipline);
    if (pa !== pb) {
      if (pa === null) return 1;
      if (pb === null) return -1;
      return pb - pa;
    }
    return a.registeredAt.localeCompare(b.registeredAt);
  });
}
