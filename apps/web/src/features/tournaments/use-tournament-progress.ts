import { useQueries, useQuery } from '@tanstack/react-query';
import { useCourts } from '@/features/courts/api';
import { drawQuery } from '@/features/draw/api';
import { useEvents } from '@/features/events/api';
import { tournamentMatchesQuery } from '@/features/matches/api';
import { scheduleQuery } from '@/features/schedule/api';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { computeProgress, drawableEvents } from './progress';

export function useTournamentProgress(tournamentId: string) {
  const courts = useCourts(tournamentId);
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const matches = useQuery(tournamentMatchesQuery(tournamentId));
  const schedule = useQuery(scheduleQuery(tournamentId));
  const playable = drawableEvents(events.data ?? []);
  const draws = useQueries({ queries: playable.map((event) => drawQuery(event.id)) });

  if (!courts.data || !events.data || !registrations.data || draws.some((d) => d.isPending)) {
    return null;
  }

  return computeProgress({
    courts: courts.data,
    events: events.data,
    registrations: registrations.data,
    draws: new Map(playable.map((event, i) => [event.id, draws[i].data ?? null])),
    matches: matches.data ?? [],
    schedule: schedule.data ?? null,
  });
}
