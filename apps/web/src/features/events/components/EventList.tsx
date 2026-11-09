import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Event } from '@/api/models';

export function EventList({ events }: { events: Event[] }) {
  const { t } = useTranslation();

  return (
    <ul className="divide-sable-200 divide-y rounded-lg bg-white shadow-sm">
      {events.map((event) => (
        <li key={event.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="min-w-0 space-y-1">
            <div className="flex items-center gap-2">
              <span className="bg-sable-200 text-sable-700 rounded px-1.5 py-0.5 text-xs font-medium">
                {event.type}
              </span>
              <span className="font-medium">{event.name}</span>
            </div>
            <p className="text-sable-600 text-sm">
              {t(`events.formats.${event.format}`)}
              {event.format === 'POOLS_THEN_ELIMINATION' &&
                ` · ${t('events.poolsSummary', {
                  size: event.poolSize,
                  qualifiers: event.qualifiersPerPool,
                })}`}
            </p>
            <p className="text-sable-600 text-xs tabular-nums">
              {t('events.scoringSummary', {
                games: event.scoring.gamesToWin,
                points: event.scoring.pointsToWin,
                cap: event.scoring.pointCap,
              })}
            </p>
          </div>
          <Link to={event.id} className="text-lagon-700 text-sm hover:underline">
            {t('events.edit')}
          </Link>
        </li>
      ))}
    </ul>
  );
}
