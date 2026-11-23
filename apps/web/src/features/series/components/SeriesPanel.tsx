import { useTranslation } from 'react-i18next';
import type { Event } from '@/api/models';
import { secondaryButtonClass } from '@/components/ui/styles';

export function SeriesPanel({
  event,
  events,
  counts,
  canSplit,
  onSplit,
  onSelect,
}: {
  event: Event;
  events: Event[];
  counts: Map<string, number>;
  canSplit: boolean;
  onSplit: () => void;
  onSelect: (eventId: string) => void;
}) {
  const { t } = useTranslation();
  const parent = events.find((e) => e.id === event.parentEventId);
  const children = events.filter((e) => e.parentEventId === event.id);

  if (parent) {
    return (
      <p className="text-sable-600 text-sm">
        {t('series.fromParent')}{' '}
        <button
          type="button"
          onClick={() => onSelect(parent.id)}
          className="text-lagon-700 font-medium hover:underline"
        >
          {parent.name}
        </button>
      </p>
    );
  }

  if (children.length > 0) {
    return (
      <section className="space-y-2 rounded-lg bg-white p-4 shadow-sm">
        <p className="text-sm font-medium">{t('series.splitInto', { count: children.length })}</p>
        <ul className="flex flex-wrap gap-2">
          {children.map((child) => (
            <li key={child.id}>
              <button
                type="button"
                onClick={() => onSelect(child.id)}
                className={`${secondaryButtonClass} text-sm`}
              >
                {child.categoryLabel ?? child.name} · {counts.get(child.id) ?? 0}
              </button>
            </li>
          ))}
        </ul>
      </section>
    );
  }

  if (!canSplit) return null;

  return (
    <section className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-4 shadow-sm">
      <div className="min-w-0">
        <p className="text-sm font-medium">{t('series.title')}</p>
        <p className="text-sable-600 text-sm">{t('series.hint')}</p>
      </div>
      <button type="button" onClick={onSplit} className={secondaryButtonClass}>
        {t('series.split')}
      </button>
    </section>
  );
}
