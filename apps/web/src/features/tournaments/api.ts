import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Tournament } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export type CreateTournamentInput = {
  organizationId: string;
  name: string;
  startsAt: string; // ISO, UTC
  endsAt: string;
  minimumRestMinutes: number;
};

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

export function useCreateTournament() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateTournamentInput) =>
      api<Tournament>('/tournaments', { method: 'POST', body: JSON.stringify(input) }),
    // La liste se recharge pour afficher le nouveau tournoi
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.tournaments }),
  });
}
