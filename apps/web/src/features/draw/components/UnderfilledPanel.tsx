import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Registration } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { registrationLabel } from '@/features/matches/labels';
import { useTransferRegistrations, type TransferResult } from '@/features/registrations/api';
import { eventsFor, suggestCategory } from '@/features/registrations/category-switch';
import { ApiError } from '@/lib/api-client';
import { useErrorMessage } from '@/lib/use-error-message';
import { MIN_EVENT_ENTRIES } from '../algorithm';
import { commonTargets, eventsToBringIn } from '../moves';

function TransferErrors({ result }: { result: TransferResult | undefined }) {
  const errorMessage = useErrorMessage();
  if (!result || result.rejected.length === 0) return null;
  return (
    <ErrorMessage>
      {result.rejected
        .map((r) => errorMessage(new ApiError(400, r.message, r.code)))
        .filter((message, index, all) => all.indexOf(message) === index)
        .join(' ')}
    </ErrorMessage>
  );
}

function MoveRow({
  tournamentId,
  registration,
  event,
  events,
}: {
  tournamentId: string;
  registration: Registration;
  event: Event;
  events: Event[];
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const transfer = useTransferRegistrations(tournamentId);
  const targets = eventsFor(registration, event, events);
  const [targetId, setTargetId] = useState(
    suggestCategory(registration, event, events)?.id ?? targets[0]?.id ?? '',
  );

  return (
    <li className="space-y-1 py-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="min-w-0 flex-1 text-sm">
          {registrationLabel(registration)}
          <span className="text-sable-600">
            {' '}
            · {t(`registrations.statuses.${registration.status}`)}
          </span>
        </span>
        {targets.length === 0 ? (
          <span className="text-sable-600 text-xs">{t('draws.underfilled.noTarget')}</span>
        ) : (
          <>
            <select
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
              className={`${inputClass} mt-0 w-auto`}
              aria-label={t('draws.underfilled.target')}
            >
              {targets.map((target) => (
                <option key={target.id} value={target.id}>
                  {target.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              disabled={!targetId || transfer.isPending}
              onClick={() =>
                transfer.mutate({ targetEventId: targetId, registrationIds: [registration.id] })
              }
              className={secondaryButtonClass}
            >
              {t('draws.underfilled.move')}
            </button>
          </>
        )}
      </div>
      {transfer.isError && <ErrorMessage>{errorMessage(transfer.error)}</ErrorMessage>}
      <TransferErrors result={transfer.data} />
    </li>
  );
}

export function UnderfilledPanel({
  tournamentId,
  event,
  events,
  registrations,
  allRegistrations,
}: {
  tournamentId: string;
  event: Event;
  events: Event[];
  registrations: Registration[];
  allRegistrations: Registration[];
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const moveAll = useTransferRegistrations(tournamentId);
  const bringIn = useTransferRegistrations(tournamentId);
  const targets = commonTargets(registrations, event, events);
  const [targetId, setTargetId] = useState(targets[0]?.id ?? '');
  const sources = eventsToBringIn(event, events, allRegistrations);

  return (
    <section className="border-sunset-200 bg-sunset-50 space-y-4 rounded-lg border p-4">
      <div>
        <h3 className="text-sunset-800 text-sm font-semibold">{t('draws.underfilled.title')}</h3>
        <p className="text-sable-700 text-sm">
          {t('draws.underfilled.hint', { min: MIN_EVENT_ENTRIES })}
        </p>
      </div>

      {sources.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-sm font-medium">{t('draws.underfilled.bringInTitle')}</h4>
          <ul className="flex flex-wrap gap-2">
            {sources.map((source) => (
              <li key={source.event.id}>
                <button
                  type="button"
                  disabled={bringIn.isPending}
                  onClick={() =>
                    bringIn.mutate({
                      targetEventId: event.id,
                      registrationIds: source.registrations.map((r) => r.id),
                    })
                  }
                  className={secondaryButtonClass}
                >
                  {t('draws.underfilled.bringIn', {
                    count: source.registrations.length,
                    event: source.event.name,
                  })}
                </button>
              </li>
            ))}
          </ul>
          {bringIn.isError && <ErrorMessage>{errorMessage(bringIn.error)}</ErrorMessage>}
          <TransferErrors result={bringIn.data} />
        </div>
      )}

      <div className="space-y-2">
        <h4 className="text-sm font-medium">{t('draws.underfilled.sendOutTitle')}</h4>
        {targets.length > 0 && registrations.length > 1 && (
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
              className={`${inputClass} mt-0 w-auto`}
              aria-label={t('draws.underfilled.target')}
            >
              {targets.map((target) => (
                <option key={target.id} value={target.id}>
                  {target.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              disabled={!targetId || moveAll.isPending}
              onClick={() =>
                moveAll.mutate({
                  targetEventId: targetId,
                  registrationIds: registrations.map((r) => r.id),
                })
              }
              className={secondaryButtonClass}
            >
              {t('draws.underfilled.moveAll', { count: registrations.length })}
            </button>
          </div>
        )}
        {moveAll.isError && <ErrorMessage>{errorMessage(moveAll.error)}</ErrorMessage>}
        <TransferErrors result={moveAll.data} />
        <ul className="divide-sunset-200 divide-y">
          {registrations.map((registration) => (
            <MoveRow
              key={registration.id}
              tournamentId={tournamentId}
              registration={registration}
              event={event}
              events={events}
            />
          ))}
        </ul>
      </div>
    </section>
  );
}
