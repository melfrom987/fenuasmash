import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Draw, DrawSettings, DrawSlotRef } from '@/api/models';
import { api, ApiError } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export const drawQuery = (eventId: string) =>
  queryOptions({
    queryKey: queryKeys.draw(eventId),
    queryFn: async () => {
      try {
        return await api<Draw>(`/events/${eventId}/draw`);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
  });

export function useDraw(eventId: string) {
  return useQuery({ ...drawQuery(eventId), enabled: eventId !== '' });
}

function useDrawMutation<TInput>(
  eventId: string,
  request: (input: TInput) => Promise<Draw | null>,
) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: request,
    onSuccess: (draw) => qc.setQueryData(queryKeys.draw(eventId), draw),
  });
}

export const useCreateDraw = (eventId: string) =>
  useDrawMutation(eventId, (settings: DrawSettings) =>
    api<Draw>(`/events/${eventId}/draw`, { method: 'POST', body: JSON.stringify(settings) }),
  );

export const useSwapDraw = (eventId: string) =>
  useDrawMutation(eventId, (swap: { a: DrawSlotRef; b: DrawSlotRef }) =>
    api<Draw>(`/events/${eventId}/draw/swap`, { method: 'POST', body: JSON.stringify(swap) }),
  );

export const usePublishDraw = (eventId: string) =>
  useDrawMutation<void>(eventId, () =>
    api<Draw>(`/events/${eventId}/draw/publish`, { method: 'POST' }),
  );

export const useDeleteDraw = (eventId: string) =>
  useDrawMutation<void>(eventId, async () => {
    await api<void>(`/events/${eventId}/draw`, { method: 'DELETE' });
    return null;
  });
