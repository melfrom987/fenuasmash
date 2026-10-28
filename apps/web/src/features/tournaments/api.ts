import { useQuery } from '@tanstack/react-query';
import type { Tournament } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useTournaments() {
  return useQuery({
    queryKey: queryKeys.tournaments,
    queryFn: () => api<Tournament[]>('/tournaments'),
  });
}

export function useTournament(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.tournament(tournamentId),
    queryFn: () => api<Tournament>(`/tournaments/${tournamentId}`),
    enabled: tournamentId !== '',
  });
}
