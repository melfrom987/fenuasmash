export const queryKeys = {
  tournaments: ['tournaments'] as const,
  tournament: (tournamentId: string) => ['tournaments', tournamentId] as const,
};
