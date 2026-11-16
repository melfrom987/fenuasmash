import type { PlayerSummary } from '@/api/models';

export const playerName = (player: Pick<PlayerSummary, 'firstName' | 'lastName'>) =>
  `${player.lastName.toUpperCase()} ${player.firstName}`;
