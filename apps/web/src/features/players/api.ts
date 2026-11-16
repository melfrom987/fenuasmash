import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Gender, Player, PlayerPage } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import { PLAYERS_PAGE_SIZE } from './constants';
import type { PlayerInput } from './schema';

export type PlayerFilters = {
  q: string;
  clubId: string; // '' = tous les clubs
  gender: Gender | ''; // '' = tous
  page: number; // à partir de 0
};

export function usePlayers(filters: PlayerFilters) {
  return useQuery({
    queryKey: queryKeys.players(filters),
    queryFn: () => {
      const params = new URLSearchParams({
        page: String(filters.page),
        size: String(PLAYERS_PAGE_SIZE),
      });
      if (filters.q) params.set('q', filters.q);
      if (filters.clubId) params.set('clubId', filters.clubId);
      if (filters.gender) params.set('gender', filters.gender);
      return api<PlayerPage>(`/players?${params.toString()}`);
    },
    // Garde la page affichée pendant le chargement de la suivante (pas de clignotement)
    placeholderData: keepPreviousData,
  });
}

export function usePlayer(playerId: string) {
  return useQuery({
    queryKey: queryKeys.player(playerId),
    queryFn: () => api<Player>(`/players/${playerId}`),
    enabled: playerId !== '',
  });
}

export function useCreatePlayer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: PlayerInput) =>
      api<Player>('/players', { method: 'POST', body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.playersAll }),
  });
}

export function useUpdatePlayer(playerId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: PlayerInput) =>
      api<Player>(`/players/${playerId}`, { method: 'PATCH', body: JSON.stringify(input) }),
    onSuccess: (player) => {
      qc.setQueryData(queryKeys.player(playerId), player);
      return qc.invalidateQueries({ queryKey: queryKeys.playersAll });
    },
  });
}
