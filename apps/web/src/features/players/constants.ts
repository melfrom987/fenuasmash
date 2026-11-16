import type { Gender, PlayerRankings } from '@/api/models';

export const GENDERS = ['MALE', 'FEMALE'] as const satisfies readonly Gender[];

export const RANKING_DISCIPLINES = [
  'singles',
  'doubles',
  'mixed',
] as const satisfies readonly (keyof PlayerRankings)[];

export const PLAYERS_PAGE_SIZE = 20;
