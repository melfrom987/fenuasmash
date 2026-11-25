import { useMemo } from 'react';
import { useCourts } from '@/features/courts/api';
import { useEvents } from '@/features/events/api';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { registrationLabel, type MatchLabels } from './labels';

export function useMatchLabels(tournamentId: string): MatchLabels {
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const courts = useCourts(tournamentId);

  return useMemo(() => {
    const entries = new Map((registrations.data ?? []).map((r) => [r.id, registrationLabel(r)]));
    const eventNames = new Map((events.data ?? []).map((e) => [e.id, e.name]));
    const courtNumbers = new Map((courts.data ?? []).map((c) => [c.id, c.number]));
    return {
      entry: (id) => (id === null ? null : (entries.get(id) ?? '?')),
      event: (id) => eventNames.get(id) ?? '?',
      court: (id) => (id === null ? null : (courtNumbers.get(id) ?? null)),
    };
  }, [registrations.data, events.data, courts.data]);
}
