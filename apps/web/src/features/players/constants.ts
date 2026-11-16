import type { Gender, PlayerRankings } from '@/api/models';

export const GENDERS = ['MALE', 'FEMALE'] as const satisfies readonly Gender[];

/** Un classement par famille de disciplines */
export const RANKING_DISCIPLINES = [
  'singles',
  'doubles',
  'mixed',
] as const satisfies readonly (keyof PlayerRankings)[];

/** Taille d'une page de la liste des joueurs */
export const PLAYERS_PAGE_SIZE = 20;
