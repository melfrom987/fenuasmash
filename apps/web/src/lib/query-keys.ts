import type { PlayerFilters } from '@/features/players/api';

export const queryKeys = {
  organizations: ['organizations'] as const,
  tournaments: ['tournaments'] as const,
  tournament: (tournamentId: string) => ['tournaments', tournamentId] as const,
  courts: (tournamentId: string) => ['tournaments', tournamentId, 'courts'] as const,
  events: (tournamentId: string) => ['tournaments', tournamentId, 'events'] as const,
  event: (eventId: string) => ['events', eventId] as const,
  clubs: ['clubs'] as const,
  club: (clubId: string) => ['clubs', clubId] as const,
  /** Préfixe commun : invalider playersAll rafraîchit listes et fiches */
  playersAll: ['players'] as const,
  players: (filters: PlayerFilters) => ['players', 'list', filters] as const,
  player: (playerId: string) => ['players', 'detail', playerId] as const,
};
