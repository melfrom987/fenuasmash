import { useTranslation } from 'react-i18next';
import type { CourtAssignment, Match, MatchStatus } from '@/api/models';
import type { MatchLabels } from '../labels';
import { MatchActions } from './MatchActions';
import { MatchHeading, MatchSides } from './MatchSides';
import { MatchStatusBadge } from './MatchStatusBadge';
import type { ResultInput } from './ResultForm';

const minutesSince = (iso: string, now: number) =>
  Math.max(0, Math.floor((now - new Date(iso).getTime()) / 60_000));

export function CourtCard({
  assignment,
  labels,
  now,
  selected,
  nextMatch,
  pending,
  onAssign,
  onStatus,
  onMove,
}: {
  assignment: CourtAssignment;
  labels: MatchLabels;
  now: number;
  selected: Match | null;
  nextMatch: Match | null;
  pending: boolean;
  onAssign: (match: Match) => void;
  onStatus: (match: Match, status: MatchStatus, result?: ResultInput) => void;
  onMove: (match: Match) => void;
}) {
  const { t } = useTranslation();
  const { court, match } = assignment;
  const free = match === null;
  const target = free && court.active && selected !== null;

  const since =
    match &&
    (match.status === 'IN_PROGRESS' && match.startedAt
      ? t('matches.playingFor', { count: minutesSince(match.startedAt, now) })
      : t('matches.calledFor', { count: minutesSince(match.updatedAt, now) }));

  return (
    <li
      className={`flex flex-col gap-3 rounded-lg bg-white p-4 shadow-sm ${
        target ? 'ring-lagon-400 ring-2' : ''
      } ${court.active ? '' : 'opacity-60'}`}
    >
      <div className="flex items-center justify-between gap-2">
        <h3 className="font-display font-semibold">
          {t('courts.label', { number: court.number })}
        </h3>
        {match ? (
          <MatchStatusBadge status={match.status} />
        ) : (
          <span className="text-sable-600 text-xs">
            {court.active ? t('matches.courtFree') : t('courts.inactive')}
          </span>
        )}
      </div>

      {match && (
        <>
          <div>
            <MatchHeading match={match} labels={labels} />
            <MatchSides match={match} labels={labels} />
            <p className="text-sable-600 mt-1 text-xs tabular-nums">{since}</p>
          </div>
          <MatchActions
            match={match}
            labels={labels}
            pending={pending}
            onStatus={(status, result) => onStatus(match, status, result)}
            onMove={() => onMove(match)}
          />
        </>
      )}

      {target && selected && (
        <button
          type="button"
          disabled={pending}
          onClick={() => onAssign(selected)}
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {selected.status === 'CALLED' ? t('matches.moveHere') : t('matches.callHere')}
        </button>
      )}

      {free && court.active && !selected && nextMatch && (
        <button
          type="button"
          disabled={pending}
          onClick={() => onAssign(nextMatch)}
          className="border-lagon-300 text-lagon-800 hover:bg-lagon-50 rounded-md border border-dashed px-3 py-2 text-left text-sm disabled:opacity-50"
        >
          <span className="block font-medium">{t('matches.callNext')}</span>
          <span className="text-sable-600 block truncate text-xs">
            {labels.side(nextMatch, 'A')?.name} {t('matches.versus')}{' '}
            {labels.side(nextMatch, 'B')?.name}
          </span>
        </button>
      )}
    </li>
  );
}
