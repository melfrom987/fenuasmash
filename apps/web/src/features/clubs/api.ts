import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Club } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { ClubInput } from './schema';

export function useClubs() {
  return useQuery({
    queryKey: queryKeys.clubs,
    queryFn: () => api<Club[]>('/clubs'),
  });
}

export function useClub(clubId: string) {
  return useQuery({
    queryKey: queryKeys.club(clubId),
    queryFn: () => api<Club>(`/clubs/${clubId}`),
    enabled: clubId !== '',
  });
}

function useInvalidateClub() {
  const qc = useQueryClient();
  return async (club: Club) => {
    qc.setQueryData(queryKeys.club(club.id), club);
    await qc.invalidateQueries({ queryKey: queryKeys.clubs, exact: true });
    await qc.invalidateQueries({ queryKey: queryKeys.playersAll });
  };
}

export function useCreateClub() {
  const invalidate = useInvalidateClub();
  return useMutation({
    mutationFn: (input: ClubInput) =>
      api<Club>('/clubs', { method: 'POST', body: JSON.stringify(input) }),
    onSuccess: invalidate,
  });
}

export function useUpdateClub(clubId: string) {
  const invalidate = useInvalidateClub();
  return useMutation({
    mutationFn: (input: ClubInput) =>
      api<Club>(`/clubs/${clubId}`, { method: 'PATCH', body: JSON.stringify(input) }),
    onSuccess: invalidate,
  });
}

export function useUploadClubLogo() {
  const invalidate = useInvalidateClub();
  return useMutation({
    mutationFn: ({ clubId, file }: { clubId: string; file: File }) => {
      const body = new FormData();
      body.append('file', file);
      return api<Club>(`/clubs/${clubId}/logo`, { method: 'PUT', body });
    },
    onSuccess: invalidate,
  });
}

export function useDeleteClubLogo() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (clubId: string) => api<void>(`/clubs/${clubId}/logo`, { method: 'DELETE' }),
    onSuccess: async (_, clubId) => {
      await qc.invalidateQueries({ queryKey: queryKeys.club(clubId) });
      await qc.invalidateQueries({ queryKey: queryKeys.clubs, exact: true });
      await qc.invalidateQueries({ queryKey: queryKeys.playersAll });
    },
  });
}
