import { useTranslation } from 'react-i18next';
import type { Event } from '@/api/models';

/** Liste des tableaux avec leur nombre d'inscrits ; le tableau choisi est mis en avant */
export function EventPicker({
  events,
  counts,
  selectedId,
  onSelect,
}: {
  events: Event[];
  counts: Map<string, number>;
  selectedId: string | undefined;
  onSelect: (eventId: string) => void;
}) {
  const { t } = useTranslation();

  return (
    <nav aria-label={t('registrations.eventsLabel')}>
      <ul className="space-y-1">
        {events.map((event) => {
          const count = counts.get(event.id) ?? 0;
          const full = event.maxEntries !== null && count >= event.maxEntries;
          const selected = event.id === selectedId;
          return (
            <li key={event.id}>
              <button
                type="button"
                onClick={() => onSelect(event.id)}
                aria-current={selected ? 'true' : undefined}
                className={`flex w-full items-center justify-between gap-2 rounded-md px-3 py-2 text-left text-sm ${
                  selected ? 'bg-lagon-600 text-white' : 'hover:bg-sable-100 bg-white'
                }`}
              >
                <span className="flex min-w-0 items-center gap-2">
                  <span
                    className={`rounded px-1.5 py-0.5 text-xs font-medium ${
                      selected ? 'bg-lagon-700' : 'bg-sable-200 text-sable-700'
                    }`}
                  >
                    {event.discipline}
                  </span>
                  <span className="truncate">{event.name}</span>
                </span>
                <span
                  className={`shrink-0 text-xs tabular-nums ${
                    full && !selected ? 'font-semibold text-red-700' : ''
                  }`}
                >
                  {event.maxEntries === null ? count : `${count}/${event.maxEntries}`}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
