export const queryKeys = {
  organizations: ['organizations'] as const,
  tournaments: ['tournaments'] as const,
  tournament: (tournamentId: string) => ['tournaments', tournamentId] as const,
};
