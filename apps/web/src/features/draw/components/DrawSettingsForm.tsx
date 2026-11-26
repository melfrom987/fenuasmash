import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { DrawFormat, DrawSettings } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import {
  defaultSeedCount,
  MAX_POOL_SIZE,
  MIN_EVENT_ENTRIES,
  MIN_POOL_SIZE,
  nextPowerOfTwo,
  POOL_NAMES,
  poolSizes,
  suggestedPoolCount,
} from '../algorithm';

const toNumber = (value: string) => (value.trim() === '' ? Number.NaN : Number(value));

export function DrawSettingsForm({
  format,
  entryCount,
  excludedCount,
  defaults,
  pending,
  error,
  onSubmit,
  onCancel,
}: {
  format: DrawFormat;
  entryCount: number;
  excludedCount: number;
  defaults: DrawSettings;
  pending: boolean;
  error: string | null;
  onSubmit: (settings: DrawSettings) => void;
  onCancel?: () => void;
}) {
  const { t } = useTranslation();
  const [poolCount, setPoolCount] = useState(
    String(defaults.poolCount ?? suggestedPoolCount(entryCount)),
  );
  const [qualifiers, setQualifiers] = useState(String(defaults.qualifiersPerPool ?? 2));
  const [seeds, setSeeds] = useState(
    String(defaults.seedCount ?? defaultSeedCount(Math.max(entryCount, 2))),
  );

  const pools = format === 'SINGLE_POOL' ? 1 : toNumber(poolCount);
  const qualifiersPerPool = toNumber(qualifiers);
  const seedCount = toNumber(seeds);
  const usesPools = format !== 'SINGLE_ELIMINATION';
  const enough = entryCount >= MIN_EVENT_ENTRIES;

  const sizes =
    usesPools && pools >= 1 && pools <= POOL_NAMES.length ? poolSizes(entryCount, pools) : [];
  const smallest = sizes.length > 0 ? Math.min(...sizes) : 0;
  const largest = sizes.length > 0 ? Math.max(...sizes) : 0;
  const poolsValid = sizes.length > 0 && largest <= MAX_POOL_SIZE && smallest >= MIN_POOL_SIZE;
  const qualifiersValid = pools === 1 || (qualifiersPerPool >= 1 && qualifiersPerPool < smallest);
  const seedsValid = seedCount >= 0 && seedCount <= entryCount;
  const qualified = pools > 1 && qualifiersValid ? pools * qualifiersPerPool : 0;

  const valid = enough && (usesPools ? poolsValid && qualifiersValid : seedsValid);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!valid) return;
    onSubmit({
      poolCount: usesPools ? pools : null,
      qualifiersPerPool: usesPools && pools > 1 ? qualifiersPerPool : null,
      seedCount: usesPools ? null : seedCount,
    });
  };

  return (
    <form onSubmit={submit} className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
      <div>
        <h3 className="font-display text-base font-semibold">{t('draws.settings.title')}</h3>
        <p className="text-sable-600 text-sm">
          {t(`rules.drawFormats.${format}`)} · {t('draws.settings.entries', { count: entryCount })}
        </p>
        {excludedCount > 0 && (
          <p className="text-sunset-800 mt-1 text-sm">
            {t('draws.settings.excluded', { count: excludedCount })}
          </p>
        )}
      </div>

      {error && <ErrorMessage>{error}</ErrorMessage>}
      {!enough && <p className="text-sm text-red-700">{t('draws.settings.notEnough')}</p>}

      {enough && format === 'POOLS_THEN_ELIMINATION' && (
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block">
            <span className="text-sable-800 text-sm font-medium">
              {t('draws.settings.poolCount')}
            </span>
            <input
              type="number"
              min={1}
              max={POOL_NAMES.length}
              value={poolCount}
              onChange={(e) => setPoolCount(e.target.value)}
              className={`${inputClass} tabular-nums`}
            />
          </label>
          <label className="block">
            <span className="text-sable-800 text-sm font-medium">
              {t('draws.settings.qualifiers')}
            </span>
            <input
              type="number"
              min={1}
              value={qualifiers}
              disabled={pools === 1}
              onChange={(e) => setQualifiers(e.target.value)}
              className={`${inputClass} tabular-nums`}
            />
          </label>
        </div>
      )}

      {enough && format === 'SINGLE_ELIMINATION' && (
        <label className="block sm:max-w-xs">
          <span className="text-sable-800 text-sm font-medium">{t('draws.settings.seeds')}</span>
          <input
            type="number"
            min={0}
            max={entryCount}
            value={seeds}
            onChange={(e) => setSeeds(e.target.value)}
            className={`${inputClass} tabular-nums`}
          />
        </label>
      )}

      {enough && (
        <ul className="bg-sable-50 text-sable-800 space-y-1 rounded-md p-3 text-sm">
          {usesPools && poolsValid && pools === 1 && (
            <li>{t('draws.settings.singlePool', { count: entryCount })}</li>
          )}
          {usesPools && poolsValid && pools > 1 && (
            <li>{t('draws.settings.poolSizes', { count: pools, sizes: sizes.join(', ') })}</li>
          )}
          {usesPools && !poolsValid && (
            <li className="text-red-700">
              {t('draws.settings.invalidPools', { min: MIN_POOL_SIZE, max: MAX_POOL_SIZE })}
            </li>
          )}
          {usesPools && poolsValid && !qualifiersValid && (
            <li className="text-red-700">{t('draws.settings.invalidQualifiers')}</li>
          )}
          {qualified > 0 && (
            <li>
              {t('draws.settings.finalPhase', {
                count: qualified,
                size: nextPowerOfTwo(qualified),
                byes: nextPowerOfTwo(qualified) - qualified,
              })}
            </li>
          )}
          {!usesPools && (
            <li>
              {t('draws.settings.bracket', {
                size: nextPowerOfTwo(entryCount),
                byes: nextPowerOfTwo(entryCount) - entryCount,
              })}
            </li>
          )}
          <li className="text-sable-600">{t('draws.settings.rules')}</li>
        </ul>
      )}

      <div className="flex justify-end gap-2">
        {onCancel && (
          <button type="button" onClick={onCancel} className={secondaryButtonClass}>
            {t('common.cancel')}
          </button>
        )}
        <Button type="submit" disabled={!valid || pending}>
          {pending ? t('draws.settings.drawing') : t('draws.settings.draw')}
        </Button>
      </div>
    </form>
  );
}
