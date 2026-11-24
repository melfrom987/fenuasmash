import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { SeriesSplitRequest, SeriesSplitResult } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useSplitIntoSeries(tournamentId: string, eventId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: SeriesSplitRequest) =>
      api<SeriesSplitResult>(`/events/${eventId}/series`, {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: async () => {
      qc.setQueryData(queryKeys.draw(eventId), null);
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.events(tournamentId) }),
        qc.invalidateQueries({ queryKey: queryKeys.registrations(tournamentId) }),
      ]);
    },
  });
}
