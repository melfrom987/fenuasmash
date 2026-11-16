import type { PlayerFilters } from '@/features/players/api';
import type { EligiblePlayersParams } from '@/features/registrations/api';

export const queryKeys = {
  organizations: ['organizations'] as const,
  tournaments: ['tournaments'] as const,
  tournament: (tournamentId: string) => ['tournaments', tournamentId] as const,
  courts: (tournamentId: string) => ['tournaments', tournamentId, 'courts'] as const,
  events: (tournamentId: string) => ['tournaments', tournamentId, 'events'] as const,
  event: (eventId: string) => ['events', eventId] as const,
  registrations: (tournamentId: string) => ['tournaments', tournamentId, 'registrations'] as const,
  eligiblePlayersAll: (eventId: string) => ['events', eventId, 'eligible-players'] as const,
  eligiblePlayers: (eventId: string, params: EligiblePlayersParams) =>
    ['events', eventId, 'eligible-players', params] as const,
  clubs: ['clubs'] as const,
  club: (clubId: string) => ['clubs', clubId] as const,
  playersAll: ['players'] as const,
  players: (filters: PlayerFilters) => ['players', 'list', filters] as const,
  player: (playerId: string) => ['players', 'detail', playerId] as const,
};
