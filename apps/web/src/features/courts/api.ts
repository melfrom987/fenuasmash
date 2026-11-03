import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Court } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useCourts(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.courts(tournamentId),
    queryFn: () => api<Court[]>(`/tournaments/${tournamentId}/courts`),
    enabled: tournamentId !== '',
  });
}

function useInvalidateCourts(tournamentId: string) {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: queryKeys.courts(tournamentId) });
}

export function useAddCourt(tournamentId: string) {
  const invalidate = useInvalidateCourts(tournamentId);
  return useMutation({
    mutationFn: () => api<Court>(`/tournaments/${tournamentId}/courts`, { method: 'POST' }),
    onSuccess: invalidate,
  });
}

export function useRemoveCourt(tournamentId: string) {
  const invalidate = useInvalidateCourts(tournamentId);
  return useMutation({
    mutationFn: (courtId: string) => api<null>(`/courts/${courtId}`, { method: 'DELETE' }),
    onSuccess: invalidate,
  });
}

export function useUpdateCourt(tournamentId: string) {
  const invalidate = useInvalidateCourts(tournamentId);
  return useMutation({
    mutationFn: ({ courtId, active }: { courtId: string; active: boolean }) =>
      api<Court>(`/courts/${courtId}`, { method: 'PATCH', body: JSON.stringify({ active }) }),
    onSuccess: invalidate,
  });
}
