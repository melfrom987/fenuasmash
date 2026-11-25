import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PairingCode, PairTabletResponse, Tablet } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useTablets(tournamentId: string, refetchInterval: number) {
  return useQuery({
    queryKey: queryKeys.tablets(tournamentId),
    queryFn: () => api<Tablet[]>(`/tournaments/${tournamentId}/tablets`),
    enabled: tournamentId !== '',
    refetchInterval,
  });
}

export function useCreatePairingCode() {
  return useMutation({
    mutationFn: (courtId: string) =>
      api<PairingCode>(`/courts/${courtId}/pairing-code`, { method: 'POST' }),
  });
}

export function useRevokeTablet(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (tabletId: string) =>
      api<Tablet>(`/tablets/${tabletId}/revoke`, { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.tablets(tournamentId) }),
  });
}

export function useSimulatePairing(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (code: string) =>
      api<PairTabletResponse>('/tablets/pair', {
        method: 'POST',
        body: JSON.stringify({ code, deviceName: 'Galaxy Tab A8' }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.tablets(tournamentId) }),
  });
}
