import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { DrawEntry, Event, SeriesSplit } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { seriesSizes } from '@/features/draw/algorithm';
import { EntryLabel } from '@/features/draw/components/EntryLabel';
import { MAX_SERIES, MIN_SERIES_ENTRIES, seriesEventName } from '../naming';

const toNumber = (value: string) => (value.trim() === '' ? Number.NaN : Number(value));

export function SeriesSplitForm({
  event,
  entries,
  excludedCount,
  pending,
  error,
  onSubmit,
  onCancel,
}: {
  event: Event;
  entries: DrawEntry[];
  excludedCount: number;
  pending: boolean;
  error: string | null;
  onSubmit: (series: SeriesSplit[]) => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  const defaultLabel = (index: number) => t('series.defaultLabel', { number: index + 1 });
  const maxSeries = Math.max(
    2,
    Math.min(MAX_SERIES, Math.floor(entries.length / MIN_SERIES_ENTRIES)),
  );

  const [count, setCount] = useState(2);
  const [sizes, setSizes] = useState(() => seriesSizes(entries.length, 2).map(String));
  const [labels, setLabels] = useState(() => [defaultLabel(0), defaultLabel(1)]);

  const changeCount = (next: number) => {
    setCount(next);
    setSizes(seriesSizes(entries.length, next).map(String));
    setLabels(Array.from({ length: next }, (_, i) => labels[i] ?? defaultLabel(i)));
  };

  const firstSizes = sizes.slice(0, count - 1).map(toNumber);
  const lastSize = entries.length - firstSizes.reduce((sum, size) => sum + (size || 0), 0);
  const allSizes = [...firstSizes, lastSize];
  const sizesValid = allSizes.every((size) => Number.isInteger(size) && size >= MIN_SERIES_ENTRIES);
  const trimmed = labels.map((label) => label.trim().toLowerCase());
  const labelsFilled = trimmed.every((label) => label !== '');
  const labelsUnique = new Set(trimmed).size === trimmed.length;
  const valid = sizesValid && labelsFilled && labelsUnique;

  const starts = allSizes.map((_, i) => allSizes.slice(0, i).reduce((sum, size) => sum + size, 0));
  const groups = allSizes.map((size, i) => entries.slice(starts[i], starts[i] + size));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!valid) return;
    onSubmit(
      groups.map((group, i) => ({
        label: labels[i].trim(),
        registrationIds: group.map((entry) => entry.registrationId),
      })),
    );
  };

  return (
    <form onSubmit={submit} className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
      <div>
        <h3 className="font-display text-base font-semibold">{t('series.title')}</h3>
        <p className="text-sable-600 text-sm">{t('series.formHint', { count: entries.length })}</p>
        {excludedCount > 0 && (
          <p className="text-sunset-800 mt-1 text-sm">
            {t('series.excluded', { count: excludedCount })}
          </p>
        )}
      </div>

      {error && <ErrorMessage>{error}</ErrorMessage>}

      <label className="block sm:max-w-xs">
        <span className="text-sable-800 text-sm font-medium">{t('series.count')}</span>
        <select
          value={count}
          onChange={(e) => changeCount(Number(e.target.value))}
          className={inputClass}
        >
          {Array.from({ length: maxSeries - 1 }, (_, i) => i + 2).map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </label>

      <div className="space-y-2">
        {labels.map((label, i) => (
          <div key={i} className="grid grid-cols-[minmax(0,1fr)_7rem] items-end gap-3">
            <label className="block">
              <span className="text-sable-800 text-sm font-medium">
                {t('series.label', { number: i + 1 })}
              </span>
              <input
                value={label}
                onChange={(e) => setLabels(labels.map((l, j) => (j === i ? e.target.value : l)))}
                className={inputClass}
              />
            </label>
            {i < count - 1 ? (
              <label className="block">
                <span className="text-sable-800 text-sm font-medium">{t('series.size')}</span>
                <input
                  type="number"
                  min={MIN_SERIES_ENTRIES}
                  value={sizes[i]}
                  onChange={(e) => setSizes(sizes.map((s, j) => (j === i ? e.target.value : s)))}
                  className={`${inputClass} tabular-nums`}
                />
              </label>
            ) : (
              <div>
                <span className="text-sable-800 text-sm font-medium">{t('series.size')}</span>
                <p className="mt-1 py-2 text-sm tabular-nums">
                  {t('series.remainder', { count: lastSize })}
                </p>
              </div>
            )}
          </div>
        ))}
      </div>

      {!sizesValid && (
        <p className="text-sm text-red-700">{t('series.tooSmall', { min: MIN_SERIES_ENTRIES })}</p>
      )}
      {!labelsFilled && <p className="text-sm text-red-700">{t('series.emptyLabel')}</p>}
      {labelsFilled && !labelsUnique && (
        <p className="text-sm text-red-700">{t('series.duplicateLabel')}</p>
      )}

      {sizesValid && (
        <div className="grid gap-3 md:grid-cols-2">
          {groups.map((group, i) => (
            <section key={i} className="border-sable-200 rounded-md border">
              <header className="bg-sable-50 border-sable-200 border-b px-3 py-2">
                <p className="text-sm font-semibold">
                  {seriesEventName(event.name, labels[i] || defaultLabel(i))}
                </p>
                <p className="text-sable-600 text-xs">
                  {t('series.entries', { count: group.length })}
                </p>
              </header>
              <ol className="divide-sable-100 divide-y">
                {group.map((entry, j) => (
                  <li
                    key={entry.registrationId}
                    className="flex items-center gap-3 px-3 py-1.5 text-sm"
                  >
                    <span className="text-sable-500 w-6 shrink-0 text-right tabular-nums">
                      {starts[i] + j + 1}
                    </span>
                    <span className="min-w-0 flex-1">
                      <EntryLabel entry={entry} />
                    </span>
                    <span className="text-sable-600 shrink-0 text-xs tabular-nums">
                      {entry.points === null
                        ? t('series.unranked')
                        : t('series.points', { count: entry.points })}
                    </span>
                  </li>
                ))}
              </ol>
            </section>
          ))}
        </div>
      )}

      <p className="text-sable-600 text-sm">{t('series.afterSplit')}</p>

      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className={secondaryButtonClass}>
          {t('common.cancel')}
        </button>
        <Button type="submit" disabled={!valid || pending}>
          {pending ? t('series.splitting') : t('series.submit')}
        </Button>
      </div>
    </form>
  );
}
