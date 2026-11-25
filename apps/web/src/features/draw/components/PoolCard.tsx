import { useTranslation } from 'react-i18next';
import type { DrawSlotRef, Pool } from '@/api/models';
import { EntryLabel } from './EntryLabel';

export function PoolCard({
  pool,
  editable,
  selected,
  onSelect,
}: {
  pool: Pool;
  editable: boolean;
  selected: DrawSlotRef | null;
  onSelect: (ref: DrawSlotRef) => void;
}) {
  const { t } = useTranslation();
  const isSelected = (index: number) =>
    selected !== null &&
    'pool' in selected &&
    selected.pool === pool.name &&
    selected.index === index;

  return (
    <section className="rounded-lg bg-white shadow-sm">
      <header className="border-sable-200 flex items-baseline justify-between border-b px-4 py-2">
        <h3 className="font-display font-semibold">{t('draws.pool', { name: pool.name })}</h3>
        {pool.qualifiers > 0 && (
          <span className="text-sable-600 text-xs">
            {t('draws.qualifiersCount', { count: pool.qualifiers })}
          </span>
        )}
      </header>
      <ol className="divide-sable-100 divide-y">
        {pool.entries.map((entry, index) => (
          <li key={entry.registrationId}>
            <button
              type="button"
              disabled={!editable}
              onClick={() => onSelect({ pool: pool.name, index })}
              className={`flex w-full items-center justify-between gap-2 px-4 py-2 text-left text-sm ${
                isSelected(index) ? 'bg-lagon-100' : editable ? 'hover:bg-sable-50' : ''
              }`}
            >
              <EntryLabel entry={entry} />
              <span className="text-sable-500 shrink-0 text-xs tabular-nums">
                {entry.points === null
                  ? t('registrations.unranked')
                  : t('registrations.points', { count: entry.points })}
              </span>
            </button>
          </li>
        ))}
      </ol>
    </section>
  );
}
