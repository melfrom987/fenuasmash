import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PlayerPage, Registration } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import { PICKER_PAGE_SIZE } from './constants';

export type EligiblePlayersParams = {
  q: string;
  partnerOf?: string;
};

export type CreateRegistrationInput = {
  playerId: string;
  partnerId?: string | null;
};

export type UpdateRegistrationInput = { partnerId: string | null } | { confirm: true };

export function useTournamentRegistrations(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.registrations(tournamentId),
    queryFn: () => api<Registration[]>(`/tournaments/${tournamentId}/registrations`),
    enabled: tournamentId !== '',
  });
}

export function useEligiblePlayers(eventId: string, params: EligiblePlayersParams) {
  return useQuery({
    queryKey: queryKeys.eligiblePlayers(eventId, params),
    queryFn: () => {
      const search = new URLSearchParams({ page: '0', size: String(PICKER_PAGE_SIZE) });
      if (params.q) search.set('q', params.q);
      if (params.partnerOf) search.set('partnerOf', params.partnerOf);
      return api<PlayerPage>(`/events/${eventId}/eligible-players?${search.toString()}`);
    },
    placeholderData: keepPreviousData,
  });
}

function useRefreshRegistrations(tournamentId: string) {
  const qc = useQueryClient();
  return async (eventId: string) => {
    await qc.invalidateQueries({ queryKey: queryKeys.registrations(tournamentId) });
    await qc.invalidateQueries({ queryKey: queryKeys.eligiblePlayersAll(eventId) });
  };
}

export function useCreateRegistration(tournamentId: string, eventId: string) {
  const refresh = useRefreshRegistrations(tournamentId);
  return useMutation({
    mutationFn: (input: CreateRegistrationInput) =>
      api<Registration>(`/events/${eventId}/registrations`, {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: () => refresh(eventId),
  });
}

export function useUpdateRegistration(tournamentId: string) {
  const refresh = useRefreshRegistrations(tournamentId);
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: UpdateRegistrationInput }) =>
      api<Registration>(`/registrations/${id}`, { method: 'PATCH', body: JSON.stringify(input) }),
    onSuccess: (registration) => refresh(registration.eventId),
  });
}

export function useDeleteRegistration(tournamentId: string) {
  const refresh = useRefreshRegistrations(tournamentId);
  return useMutation({
    mutationFn: (registration: Registration) =>
      api<void>(`/registrations/${registration.id}`, { method: 'DELETE' }),
    onSuccess: (_, registration) => refresh(registration.eventId),
  });
}
