import type { PlayerSummary } from '@/api/models';

/** « TERIITAHI Teva » : nom en capitales, comme sur les feuilles de match */
export const playerName = (player: Pick<PlayerSummary, 'firstName' | 'lastName'>) =>
  `${player.lastName.toUpperCase()} ${player.firstName}`;
