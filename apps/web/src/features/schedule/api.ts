import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Schedule, ScheduleGenerateRequest, ScheduleMoveRequest } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export const scheduleQuery = (tournamentId: string) =>
  queryOptions({
    queryKey: queryKeys.schedule(tournamentId),
    queryFn: () => api<Schedule>(`/tournaments/${tournamentId}/schedule`),
  });

export function useSchedule(tournamentId: string) {
  return useQuery(scheduleQuery(tournamentId));
}

function useScheduleMutation<TInput>(
  tournamentId: string,
  request: (input: TInput) => Promise<Schedule>,
) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: request,
    onSuccess: async (schedule) => {
      qc.setQueryData(queryKeys.schedule(tournamentId), schedule);
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.matches(tournamentId) }),
        qc.invalidateQueries({ queryKey: queryKeys.matchQueue(tournamentId) }),
      ]);
    },
  });
}

export function useGenerateSchedule(tournamentId: string) {
  return useScheduleMutation<ScheduleGenerateRequest | void>(tournamentId, (input) =>
    api<Schedule>(`/tournaments/${tournamentId}/schedule`, {
      method: 'POST',
      body: JSON.stringify(input ?? {}),
    }),
  );
}

export function useMoveInSchedule(tournamentId: string) {
  return useScheduleMutation<ScheduleMoveRequest>(tournamentId, (input) =>
    api<Schedule>(`/tournaments/${tournamentId}/schedule/move`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  );
}

export function usePublishSchedule(tournamentId: string) {
  return useScheduleMutation<void>(tournamentId, () =>
    api<Schedule>(`/tournaments/${tournamentId}/schedule/publish`, { method: 'POST' }),
  );
}
