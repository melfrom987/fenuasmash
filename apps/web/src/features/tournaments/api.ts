import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Tournament } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { RulesInput } from './rules';

export type CreateTournamentInput = {
  organizationId: string;
  name: string;
  startsAt: string;
  endsAt: string;
  minimumRestMinutes: number;
  courtCount: number;
} & RulesInput;

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

export function useUpdateTournament(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<CreateTournamentInput>) =>
      api<Tournament>(`/tournaments/${tournamentId}`, {
        method: 'PATCH',
        body: JSON.stringify(input),
      }),
    onSuccess: (tournament) => {
      qc.setQueryData(queryKeys.tournament(tournamentId), tournament);
      return qc.invalidateQueries({ queryKey: queryKeys.tournaments });
    },
  });
}
