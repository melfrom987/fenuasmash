import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Event } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { EventInput } from './schema';

export function useEvents(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.events(tournamentId),
    queryFn: () => api<Event[]>(`/tournaments/${tournamentId}/events`),
    enabled: tournamentId !== '',
  });
}

export function useEvent(eventId: string) {
  return useQuery({
    queryKey: queryKeys.event(eventId),
    queryFn: () => api<Event>(`/events/${eventId}`),
    enabled: eventId !== '',
  });
}

export function useCreateEvent(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: EventInput) =>
      api<Event>(`/tournaments/${tournamentId}/events`, {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.events(tournamentId) }),
  });
}

export function useUpdateEvent(tournamentId: string, eventId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: EventInput) =>
      api<Event>(`/events/${eventId}`, { method: 'PATCH', body: JSON.stringify(input) }),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: queryKeys.event(eventId) });
      await qc.invalidateQueries({ queryKey: queryKeys.events(tournamentId) });
    },
  });
}

export function useDeleteEvent(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (eventId: string) => api<null>(`/events/${eventId}`, { method: 'DELETE' }),
    onSuccess: (_data, eventId) => {
      qc.removeQueries({ queryKey: queryKeys.event(eventId) });
      return qc.invalidateQueries({ queryKey: queryKeys.events(tournamentId) });
    },
  });
}
