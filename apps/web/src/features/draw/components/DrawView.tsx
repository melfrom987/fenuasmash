import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Draw, DrawSlotRef } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { formatDateTime } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';
import { clubConflicts } from '../algorithm';
import { useCreateDraw, useDeleteDraw, usePublishDraw, useSwapDraw } from '../api';
import { BracketView } from './BracketView';
import { PoolCard } from './PoolCard';

const sameKind = (a: DrawSlotRef, b: DrawSlotRef) => 'pool' in a === 'pool' in b;
const sameRef = (a: DrawSlotRef, b: DrawSlotRef) => JSON.stringify(a) === JSON.stringify(b);

export function DrawView({ draw, onEditSettings }: { draw: Draw; onEditSettings: () => void }) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const redraw = useCreateDraw(draw.eventId);
  const swap = useSwapDraw(draw.eventId);
  const publish = usePublishDraw(draw.eventId);
  const remove = useDeleteDraw(draw.eventId);
  const [selected, setSelected] = useState<DrawSlotRef | null>(null);
  const [confirmPublish, setConfirmPublish] = useState(false);

  const editable = draw.status === 'DRAFT';
  const pending = redraw.isPending || swap.isPending || publish.isPending || remove.isPending;
  const error = redraw.error ?? swap.error ?? publish.error ?? remove.error;
  const conflicts = clubConflicts(draw.pools, draw.pools.length > 0 ? null : draw.bracket);

  const select = (ref: DrawSlotRef) => {
    if (!selected || !sameKind(selected, ref)) return setSelected(ref);
    if (sameRef(selected, ref)) return setSelected(null);
    swap.mutate({ a: selected, b: ref }, { onSettled: () => setSelected(null) });
  };

  const settings = {
    poolCount: draw.pools.length > 0 ? draw.pools.length : null,
    qualifiersPerPool: draw.pools[0]?.qualifiers || null,
    seedCount: draw.pools.length === 0 ? draw.seedCount : null,
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-4 shadow-sm">
        <div>
          <span
            className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
              editable ? 'bg-sunset-100 text-sunset-800' : 'bg-lagon-100 text-lagon-800'
            }`}
          >
            {editable
              ? t('draws.status.DRAFT')
              : t('draws.publishedOn', {
                  date: formatDateTime(draw.publishedAt ?? draw.createdAt),
                })}
          </span>
          <p className="text-sable-600 mt-1 text-sm">
            {editable ? t('draws.swapHint') : t('draws.lockedHint')}
          </p>
          {editable && conflicts > 0 && (
            <p className="text-sunset-800 mt-1 text-sm">
              {t('draws.clubConflicts', { count: conflicts })}
            </p>
          )}
        </div>
        {editable && !confirmPublish && (
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={() => redraw.mutate(settings)}
              className={secondaryButtonClass}
            >
              {t('draws.redraw')}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={onEditSettings}
              className={secondaryButtonClass}
            >
              {t('draws.editSettings')}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => remove.mutate()}
              className="rounded-md px-3 py-2 text-sm text-red-700 hover:bg-red-50"
            >
              {t('draws.delete')}
            </button>
            <Button type="button" disabled={pending} onClick={() => setConfirmPublish(true)}>
              {t('draws.publish')}
            </Button>
          </div>
        )}
        {editable && confirmPublish && (
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm">{t('draws.publishConfirm')}</p>
            <button
              type="button"
              onClick={() => setConfirmPublish(false)}
              className={secondaryButtonClass}
            >
              {t('common.cancel')}
            </button>
            <Button
              type="button"
              disabled={pending}
              onClick={() =>
                publish.mutate(undefined, { onSettled: () => setConfirmPublish(false) })
              }
            >
              {t('draws.publish')}
            </Button>
          </div>
        )}
      </div>

      {error && <ErrorMessage>{errorMessage(error)}</ErrorMessage>}

      {draw.pools.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {draw.pools.map((pool) => (
            <PoolCard
              key={pool.name}
              pool={pool}
              editable={editable && !pending}
              selected={selected}
              onSelect={select}
            />
          ))}
        </div>
      )}

      {draw.bracket && (
        <div className="space-y-2">
          {draw.pools.length > 0 && (
            <h3 className="font-display text-base font-semibold">{t('draws.finalPhase')}</h3>
          )}
          <BracketView
            bracket={draw.bracket}
            editable={editable && !pending}
            selected={selected}
            onSelect={select}
          />
        </div>
      )}
    </div>
  );
}
