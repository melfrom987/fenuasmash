import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type {
  ChangeMatchStatusRequest,
  CourtAssignment,
  CreateMatchRequest,
  Match,
  QueuedMatch,
} from '@/api/models';
import { ApiError, api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import { LIVE_REFRESH_MS } from './status';

export function useTournamentMatches(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.matches(tournamentId),
    queryFn: () => api<Match[]>(`/tournaments/${tournamentId}/matches`),
    refetchInterval: LIVE_REFRESH_MS,
  });
}

export function useMatchQueue(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.matchQueue(tournamentId),
    queryFn: () => api<QueuedMatch[]>(`/tournaments/${tournamentId}/match-queue`),
    refetchInterval: LIVE_REFRESH_MS,
  });
}

export function useCourtAssignments(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.courtAssignments(tournamentId),
    queryFn: () => api<CourtAssignment[]>(`/tournaments/${tournamentId}/court-assignments`),
    refetchInterval: LIVE_REFRESH_MS,
  });
}

function useRefreshMatches(tournamentId: string) {
  const qc = useQueryClient();
  return () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: queryKeys.matches(tournamentId) }),
      qc.invalidateQueries({ queryKey: queryKeys.matchQueue(tournamentId) }),
      qc.invalidateQueries({ queryKey: queryKeys.courtAssignments(tournamentId) }),
    ]);
}

const isConflict = (error: Error) => error instanceof ApiError && error.status === 409;

export function useAssignCourt(tournamentId: string) {
  const refresh = useRefreshMatches(tournamentId);
  return useMutation({
    mutationFn: ({ match, courtId }: { match: Match; courtId: string }) =>
      api<Match>(`/matches/${match.id}/court`, {
        method: 'POST',
        body: JSON.stringify({ courtId, stateVersion: match.stateVersion }),
      }),
    onSuccess: refresh,
    onError: (error) => (isConflict(error) ? refresh() : undefined),
  });
}

export function useChangeMatchStatus(tournamentId: string) {
  const refresh = useRefreshMatches(tournamentId);
  return useMutation({
    mutationFn: ({
      match,
      ...input
    }: Omit<ChangeMatchStatusRequest, 'stateVersion'> & {
      match: Match;
    }) =>
      api<Match>(`/matches/${match.id}/status`, {
        method: 'POST',
        body: JSON.stringify({ ...input, stateVersion: match.stateVersion }),
      }),
    onSuccess: refresh,
    onError: (error) => (isConflict(error) ? refresh() : undefined),
  });
}

export function useCreateMatch(tournamentId: string) {
  const refresh = useRefreshMatches(tournamentId);
  return useMutation({
    mutationFn: ({ eventId, ...input }: CreateMatchRequest & { eventId: string }) =>
      api<Match>(`/events/${eventId}/matches`, { method: 'POST', body: JSON.stringify(input) }),
    onSuccess: refresh,
  });
}
