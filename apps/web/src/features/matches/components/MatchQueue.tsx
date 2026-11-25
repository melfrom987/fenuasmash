import { useTranslation } from 'react-i18next';
import type { Match, MatchStatus, QueuedMatch } from '@/api/models';
import type { MatchLabels } from '../labels';
import { MatchActions } from './MatchActions';
import { MatchHeading, MatchSides } from './MatchSides';
import type { ResultInput } from './ResultForm';

export function MatchQueue({
  queue,
  labels,
  selectedId,
  pending,
  onSelect,
  onStatus,
}: {
  queue: QueuedMatch[];
  labels: MatchLabels;
  selectedId: string | null;
  pending: boolean;
  onSelect: (match: Match) => void;
  onStatus: (match: Match, status: MatchStatus, result?: ResultInput) => void;
}) {
  const { t } = useTranslation();

  if (queue.length === 0) {
    return (
      <p className="text-sable-600 rounded-lg bg-white p-4 text-center text-sm shadow-sm">
        {t('matches.queueEmpty')}
      </p>
    );
  }

  return (
    <ol className="divide-sable-200 divide-y rounded-lg bg-white shadow-sm">
      {queue.map(({ match, available }, index) => (
        <li
          key={match.id}
          className={`space-y-2 px-4 py-3 ${selectedId === match.id ? 'bg-lagon-50' : ''}`}
        >
          <div className="flex gap-3">
            <span className="text-sable-500 w-5 shrink-0 text-right text-sm tabular-nums">
              {index + 1}
            </span>
            <div className="min-w-0 flex-1">
              <MatchHeading match={match} labels={labels} />
              <MatchSides match={match} labels={labels} />
              {!available && (
                <p className="text-sunset-800 mt-0.5 text-xs">{t('matches.sideBusy')}</p>
              )}
            </div>
          </div>
          <MatchActions
            match={match}
            labels={labels}
            pending={pending}
            onStatus={(status, result) => onStatus(match, status, result)}
            onCall={available ? () => onSelect(match) : undefined}
          />
        </li>
      ))}
    </ol>
  );
}
