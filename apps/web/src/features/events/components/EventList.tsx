import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Event } from '@/api/models';
import { formatDate, formatTime } from '@/lib/dates';

export function EventList({ events }: { events: Event[] }) {
  const { t } = useTranslation();

  const category = (event: Event) =>
    event.categoryType === 'OPEN'
      ? t('events.categoryTypes.OPEN')
      : `${t(`events.categoryTypes.${event.categoryType}`)} : ${event.categoryLabel}`;

  return (
    <ul className="divide-sable-200 divide-y rounded-lg bg-white shadow-sm">
      {events.map((event) => (
        <li key={event.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="min-w-0 space-y-1">
            <div className="flex items-center gap-2">
              <span className="bg-sable-200 text-sable-700 rounded px-1.5 py-0.5 text-xs font-medium">
                {event.discipline}
              </span>
              <span className="font-medium">{event.name}</span>
            </div>
            <p className="text-sable-600 text-sm">
              {[
                category(event),
                event.plannedDate &&
                  t('events.plannedOn', {
                    date: formatDate(event.plannedDate),
                    time: formatTime(event.plannedDate),
                  }),
                event.maxEntries
                  ? t('events.maxEntriesSummary', { max: event.maxEntries })
                  : t('events.unlimited'),
              ]
                .filter(Boolean)
                .join(' · ')}
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
