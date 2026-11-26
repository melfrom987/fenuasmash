import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { Match, MatchSide } from '@/api/models';
import { useCourts } from '@/features/courts/api';
import { useEvents } from '@/features/events/api';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { tournamentMatchesQuery } from './api';
import { knockoutStage, registrationLabel, type MatchLabels } from './labels';

export function useMatchLabels(tournamentId: string): MatchLabels {
  const { t } = useTranslation();
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const courts = useCourts(tournamentId);
  const matches = useQuery(tournamentMatchesQuery(tournamentId));

  return useMemo(() => {
    const entries = new Map((registrations.data ?? []).map((r) => [r.id, registrationLabel(r)]));
    const eventNames = new Map((events.data ?? []).map((e) => [e.id, e.name]));
    const courtNumbers = new Map((courts.data ?? []).map((c) => [c.id, c.number]));
    const numbers = new Map((matches.data ?? []).map((m) => [m.id, m.number ?? null]));
    const lastRound = new Map<string, number>();
    for (const m of matches.data ?? []) {
      if (m.phase === 'KNOCKOUT' && m.round !== null) {
        lastRound.set(m.eventId, Math.max(lastRound.get(m.eventId) ?? 0, m.round));
      }
    }

    const entry = (id: string | null) => (id === null ? null : (entries.get(id) ?? '?'));

    const side = (match: Match, which: MatchSide) => {
      const id = which === 'A' ? match.sideA : match.sideB;
      if (id !== null) return { name: entry(id) ?? '?', known: true };
      const source = which === 'A' ? match.sourceA : match.sourceB;
      if (!source) return null;
      if (source.kind === 'POOL_RANK') {
        return {
          name: t('draws.qualifier', { count: source.rank, ordinal: true, pool: source.pool }),
          known: false,
        };
      }
      const number = numbers.get(source.matchId) ?? '?';
      return {
        name:
          source.kind === 'WINNER'
            ? t('schedule.labels.winnerOf', { number })
            : t('schedule.labels.loserOf', { number }),
        known: false,
      };
    };

    const round = (match: Match) => {
      if (match.phase === 'THIRD_PLACE') return t('schedule.labels.thirdPlace');
      if (match.phase === 'POOL' && match.pool) {
        return t('schedule.labels.pool', { pool: match.pool, round: match.round ?? 1 });
      }
      if (match.phase === 'KNOCKOUT' && match.round !== null) {
        const last = lastRound.get(match.eventId) ?? match.round;
        const stage = knockoutStage(match.round, last);
        return stage === 'roundOf'
          ? t('schedule.labels.roundOf', { count: 2 ** (last - match.round + 1) })
          : t(`schedule.labels.${stage}`);
      }
      return match.round === null ? null : t('matches.round', { round: match.round });
    };

    return {
      entry,
      side,
      event: (id) => eventNames.get(id) ?? '?',
      court: (id) => (id === null ? null : (courtNumbers.get(id) ?? null)),
      round,
    };
  }, [registrations.data, events.data, courts.data, matches.data, t]);
}
