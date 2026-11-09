export const queryKeys = {
  organizations: ['organizations'] as const,
  tournaments: ['tournaments'] as const,
  tournament: (tournamentId: string) => ['tournaments', tournamentId] as const,
  courts: (tournamentId: string) => ['tournaments', tournamentId, 'courts'] as const,
  events: (tournamentId: string) => ['tournaments', tournamentId, 'events'] as const,
  event: (eventId: string) => ['events', eventId] as const,
};
