import { useTranslation } from 'react-i18next';
import type { Court, Match } from '@/api/models';
import type { MatchLabels } from '@/features/matches/labels';
import { formatTime } from '@/lib/dates';
import type { EventColor } from '../colors';
import type { ScheduleDay } from '../grid';
import { ScheduleCell } from './ScheduleCell';

export function ScheduleGrid({
  day,
  courts,
  extraStart,
  labels,
  selectedId,
  flagged,
  pending,
  colors,
  focusedEventId,
  onSelect,
  onPlace,
}: {
  day: ScheduleDay;
  courts: Court[];
  extraStart: string;
  labels: MatchLabels;
  selectedId: string | null;
  flagged: Set<string>;
  pending: boolean;
  colors: Map<string, EventColor>;
  focusedEventId: string | null;
  onSelect: (match: Match) => void;
  onPlace: (start: string, courtId: string) => void;
}) {
  const { t } = useTranslation();
  const rows = [...day.rows, { start: extraStart, cells: courts.map(() => null) }];

  return (
    <div className="overflow-x-auto rounded-lg bg-white shadow-sm">
      <table className="w-full min-w-max border-collapse text-sm">
        <thead>
          <tr className="border-sable-200 border-b">
            <th className="bg-sable-50 sticky left-0 w-16 px-3 py-2 text-left text-xs font-medium">
              {t('schedule.time')}
            </th>
            {courts.map((court) => (
              <th key={court.id} className="min-w-44 px-2 py-2 text-left text-xs font-medium">
                {t('courts.label', { number: court.number })}
                {!court.active && (
                  <span className="text-sable-500 font-normal"> · {t('courts.inactive')}</span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.start} className="border-sable-100 border-b last:border-0">
              <th className="bg-sable-50 sticky left-0 px-3 py-2 text-left align-top text-xs font-medium tabular-nums">
                {formatTime(row.start)}
              </th>
              {row.cells.map((match, index) => {
                const court = courts[index];
                return (
                  <td key={court.id} className="p-1.5 align-top">
                    {match ? (
                      <ScheduleCell
                        match={match}
                        labels={labels}
                        selected={match.id === selectedId}
                        flagged={flagged.has(match.id)}
                        disabled={pending}
                        color={colors.get(match.eventId)}
                        dimmed={focusedEventId !== null && focusedEventId !== match.eventId}
                        onClick={() =>
                          selectedId && selectedId !== match.id
                            ? onPlace(row.start, court.id)
                            : onSelect(match)
                        }
                      />
                    ) : selectedId && court.active ? (
                      <button
                        type="button"
                        disabled={pending}
                        onClick={() => onPlace(row.start, court.id)}
                        className="border-lagon-300 text-lagon-700 hover:bg-lagon-50 h-full min-h-16 w-full rounded-md border border-dashed text-xs disabled:opacity-50"
                      >
                        {t('schedule.placeHere')}
                      </button>
                    ) : (
                      <div className="min-h-16" />
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
