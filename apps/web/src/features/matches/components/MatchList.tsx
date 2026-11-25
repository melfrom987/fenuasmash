import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Match, MatchStatus } from '@/api/models';
import { inputClass } from '@/components/ui/styles';
import type { MatchLabels } from '../labels';
import { MATCH_STATUSES } from '../status';
import { MatchActions } from './MatchActions';
import { MatchHeading, MatchSides } from './MatchSides';
import { MatchStatusBadge } from './MatchStatusBadge';
import type { ResultInput } from './ResultForm';

const LIST_ACTION_STATUSES: MatchStatus[] = ['SCHEDULED', 'FINISHED'];

export function MatchList({
  matches,
  events,
  labels,
  pending,
  onStatus,
}: {
  matches: Match[];
  events: Event[];
  labels: MatchLabels;
  pending: boolean;
  onStatus: (match: Match, status: MatchStatus, result?: ResultInput) => void;
}) {
  const { t } = useTranslation();
  const [eventId, setEventId] = useState('');
  const [status, setStatus] = useState<MatchStatus | ''>('');

  const shown = matches.filter(
    (m) => (!eventId || m.eventId === eventId) && (!status || m.status === status),
  );

  const result = (match: Match) => {
    if (!match.winner) return null;
    const winner = labels.entry(match.winner === 'A' ? match.sideA : match.sideB);
    return match.outcome && match.outcome !== 'NORMAL'
      ? t('matches.wonBy', { winner, outcome: t(`matches.outcomes.${match.outcome}`) })
      : t('matches.won', { winner });
  };

  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h3 className="font-display text-base font-semibold">
          {t('matches.allTitle', { count: matches.length })}
        </h3>
        <div className="flex flex-wrap gap-2">
          <label className="block">
            <span className="sr-only">{t('matches.filterEvent')}</span>
            <select
              value={eventId}
              onChange={(e) => setEventId(e.target.value)}
              className={`${inputClass} mt-0 w-auto`}
            >
              <option value="">{t('matches.allEvents')}</option>
              {events.map((event) => (
                <option key={event.id} value={event.id}>
                  {event.name}
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="sr-only">{t('matches.filterStatus')}</span>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as MatchStatus | '')}
              className={`${inputClass} mt-0 w-auto`}
            >
              <option value="">{t('matches.allStatuses')}</option>
              {MATCH_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {t(`matches.statuses.${s}`)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {shown.length === 0 ? (
        <p className="text-sable-600 rounded-lg bg-white p-6 text-center text-sm shadow-sm">
          {matches.length === 0 ? t('matches.empty') : t('matches.noneWithFilter')}
        </p>
      ) : (
        <ul className="divide-sable-200 divide-y rounded-lg bg-white shadow-sm">
          {shown.map((match) => {
            const court = labels.court(match.courtId);
            return (
              <li
                key={match.id}
                className="flex flex-wrap items-start justify-between gap-3 px-4 py-3"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <MatchStatusBadge status={match.status} />
                    {court !== null && (
                      <span className="text-sable-600 text-xs">
                        {t('courts.label', { number: court })}
                      </span>
                    )}
                  </div>
                  <MatchHeading match={match} labels={labels} />
                  <MatchSides match={match} labels={labels} />
                  {result(match) && <p className="text-sable-700 text-xs">{result(match)}</p>}
                </div>
                {LIST_ACTION_STATUSES.includes(match.status) && (
                  <MatchActions
                    match={match}
                    labels={labels}
                    pending={pending}
                    onStatus={(next, input) => onStatus(match, next, input)}
                  />
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
