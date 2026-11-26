import { useTranslation } from 'react-i18next';
import type { Match } from '@/api/models';
import type { MatchLabels } from '@/features/matches/labels';
import type { EventColor } from '../colors';
import { PLANNABLE } from '../planner';

export function ScheduleCell({
  match,
  labels,
  selected,
  flagged,
  disabled,
  color,
  dimmed = false,
  onClick,
}: {
  match: Match;
  labels: MatchLabels;
  selected: boolean;
  flagged: boolean;
  disabled: boolean;
  color?: EventColor;
  dimmed?: boolean;
  onClick: () => void;
}) {
  const { t } = useTranslation();
  const movable = PLANNABLE.includes(match.status);
  const side = (which: 'A' | 'B') => {
    const label = labels.side(match, which);
    return (
      <span className={`block truncate ${label?.known === false ? 'text-sable-500 italic' : ''}`}>
        {label?.name ?? t('matches.unknownSide')}
      </span>
    );
  };

  return (
    <button
      type="button"
      disabled={disabled || !movable}
      onClick={onClick}
      title={flagged ? t('schedule.cellWarning') : undefined}
      style={
        color
          ? {
              borderLeftColor: color.strong,
              borderLeftWidth: 4,
              backgroundColor: selected || flagged || !movable ? undefined : color.soft,
            }
          : undefined
      }
      className={`w-full rounded-md border px-2 py-1.5 text-left text-xs transition ${
        dimmed ? 'opacity-30' : ''
      } ${
        selected
          ? 'border-lagon-500 bg-lagon-50 ring-lagon-400 ring-2'
          : flagged
            ? 'border-red-300 bg-red-50 hover:border-red-400'
            : movable
              ? 'border-sable-200 hover:border-lagon-400 bg-white'
              : 'border-sable-200 bg-sable-100 text-sable-500'
      }`}
    >
      <span className="text-sable-600 flex justify-between gap-1">
        <span className="truncate">
          {match.number != null && (
            <span className="text-sable-800 font-semibold">
              {t('schedule.labels.number', { number: match.number })}{' '}
            </span>
          )}
          {labels.event(match.eventId)}
        </span>
        {flagged && <span className="shrink-0 font-semibold text-red-700">!</span>}
      </span>
      <span className="text-sable-500 block truncate">{labels.round(match)}</span>
      {side('A')}
      {side('B')}
    </button>
  );
}
