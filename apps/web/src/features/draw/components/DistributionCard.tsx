import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Event, Registration } from '@/api/models';
import { registrationLabel } from '@/features/matches/labels';
import { eventsFor } from '@/features/registrations/category-switch';
import { entryPoints, sortByRanking } from '@/features/registrations/ranking';
import { MIN_EVENT_ENTRIES } from '../algorithm';
import { neighbours } from '../ladder';

const arrowClass =
  'text-sable-600 hover:bg-sable-100 flex size-7 items-center justify-center rounded disabled:opacity-25';

export function DistributionCard({
  event,
  events,
  registrations,
  locked,
  split,
  pending,
  onMove,
}: {
  event: Event;
  events: Event[];
  registrations: Registration[];
  locked: boolean;
  split: boolean;
  pending: boolean;
  onMove: (registrationIds: string[], targetEventId: string) => void;
}) {
  const { t } = useTranslation();
  const confirmed = registrations.filter((r) => r.status === 'CONFIRMED');
  const others = registrations.filter((r) => r.status !== 'CONFIRMED');
  const sorted = [
    ...sortByRanking(confirmed, event.discipline),
    ...sortByRanking(others, event.discipline),
  ];
  const { up, down } = neighbours(event, events);
  const underfilled = !split && confirmed.length < MIN_EVENT_ENTRIES && registrations.length > 0;

  return (
    <li
      className={`flex flex-col rounded-lg bg-white shadow-sm ${
        event.parentEventId ? 'border-lagon-300 border-l-4' : ''
      }`}
    >
      <header className="border-sable-200 flex items-start justify-between gap-2 border-b px-3 py-2">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{event.name}</h3>
          <p className="text-sable-600 text-xs">
            {t('draws.distribution.count', { count: confirmed.length })}
            {others.length > 0 && ` · ${t('draws.distribution.pending', { count: others.length })}`}
          </p>
        </div>
        {locked ? (
          <span className="bg-lagon-100 text-lagon-800 shrink-0 rounded px-1.5 py-0.5 text-xs">
            {t('draws.distribution.locked')}
          </span>
        ) : split ? (
          <span className="bg-sable-100 text-sable-700 shrink-0 rounded px-1.5 py-0.5 text-xs">
            {t('draws.distribution.split')}
          </span>
        ) : underfilled ? (
          <Link
            to={`../draws?event=${event.id}`}
            relative="path"
            className="bg-sunset-100 text-sunset-800 shrink-0 rounded px-1.5 py-0.5 text-xs hover:underline"
          >
            {t('draws.distribution.tooFew', { min: MIN_EVENT_ENTRIES })}
          </Link>
        ) : null}
      </header>

      {sorted.length === 0 ? (
        <p className="text-sable-500 px-3 py-3 text-xs">{t('draws.distribution.empty')}</p>
      ) : (
        <ol className="divide-sable-100 divide-y">
          {sorted.map((registration, index) => {
            const targets = eventsFor(registration, event, events);
            const canUp = up !== null && targets.some((e) => e.id === up.id);
            const canDown = down !== null && targets.some((e) => e.id === down.id);
            const points = entryPoints(registration, event.discipline);
            return (
              <li key={registration.id} className="flex items-center gap-1.5 px-2 py-1 text-sm">
                <span className="text-sable-500 w-5 shrink-0 text-right text-xs tabular-nums">
                  {registration.status === 'CONFIRMED' ? index + 1 : ''}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate">{registrationLabel(registration)}</span>
                  <span className="text-sable-500 block text-xs">
                    {points === null ? t('series.unranked') : t('series.points', { count: points })}
                    {registration.status !== 'CONFIRMED' &&
                      ` · ${t(`registrations.statuses.${registration.status}`)}`}
                  </span>
                </span>
                {!locked && (
                  <>
                    <button
                      type="button"
                      className={arrowClass}
                      disabled={!canUp || pending}
                      title={up ? t('draws.distribution.moveTo', { event: up.name }) : undefined}
                      aria-label={t('draws.distribution.up')}
                      onClick={() => up && onMove([registration.id], up.id)}
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className={arrowClass}
                      disabled={!canDown || pending}
                      title={
                        down ? t('draws.distribution.moveTo', { event: down.name }) : undefined
                      }
                      aria-label={t('draws.distribution.down')}
                      onClick={() => down && onMove([registration.id], down.id)}
                    >
                      ↓
                    </button>
                    <select
                      value=""
                      disabled={pending || targets.length === 0}
                      onChange={(e) => e.target.value && onMove([registration.id], e.target.value)}
                      className="border-sable-200 text-sable-600 w-8 rounded border bg-white text-xs"
                      aria-label={t('draws.distribution.other')}
                      title={t('draws.distribution.other')}
                    >
                      <option value="">…</option>
                      {targets.map((target) => (
                        <option key={target.id} value={target.id}>
                          {target.name}
                        </option>
                      ))}
                    </select>
                  </>
                )}
              </li>
            );
          })}
        </ol>
      )}
    </li>
  );
}
