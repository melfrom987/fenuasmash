import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Registration } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { ApiError } from '@/lib/api-client';
import { useErrorMessage } from '@/lib/use-error-message';
import { useTransferRegistrations, type TransferResult } from '../api';
import { playerName } from '../format';

export function TransferBar({
  tournamentId,
  event,
  events,
  selected,
  registrations,
  onClear,
}: {
  tournamentId: string;
  event: Event;
  events: Event[];
  selected: string[];
  registrations: Registration[];
  onClear: () => void;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const transfer = useTransferRegistrations(tournamentId);
  const [targetId, setTargetId] = useState('');
  const [result, setResult] = useState<TransferResult | null>(null);

  const targets = events.filter((e) => e.id !== event.id && e.discipline === event.discipline);
  const nameOf = (registrationId: string) => {
    const registration = registrations.find((r) => r.id === registrationId);
    return registration ? playerName(registration.player) : registrationId;
  };
  const issueText = (code: string, message: string) =>
    errorMessage(new ApiError(400, message, code));

  const move = () =>
    transfer.mutate(
      { targetEventId: targetId, registrationIds: selected },
      {
        onSuccess: (data) => {
          setResult(data);
          onClear();
        },
      },
    );

  return (
    <div className="space-y-2">
      {selected.length > 0 && (
        <div className="border-lagon-200 bg-lagon-50 flex flex-wrap items-center gap-3 rounded-lg border p-3">
          <span className="text-sm font-medium">
            {t('registrations.transfer.selected', { count: selected.length })}
          </span>
          {targets.length === 0 ? (
            <span className="text-sable-600 text-sm">{t('registrations.transfer.noTarget')}</span>
          ) : (
            <>
              <select
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                className={`${inputClass} mt-0 w-auto`}
              >
                <option value="" disabled>
                  {t('registrations.transfer.chooseEvent')}
                </option>
                {targets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.name}
                  </option>
                ))}
              </select>
              <Button type="button" disabled={!targetId || transfer.isPending} onClick={move}>
                {transfer.isPending
                  ? t('registrations.transfer.moving')
                  : t('registrations.transfer.move')}
              </Button>
            </>
          )}
          <button type="button" onClick={onClear} className={`${secondaryButtonClass} ml-auto`}>
            {t('registrations.transfer.clear')}
          </button>
        </div>
      )}

      {transfer.isError && <ErrorMessage>{errorMessage(transfer.error)}</ErrorMessage>}

      {result && (
        <div className="rounded-lg bg-white p-3 text-sm shadow-sm">
          <div className="flex items-start justify-between gap-3">
            <p className="text-lagon-800 font-medium">
              {t('registrations.transfer.moved', { count: result.moved.length })}
            </p>
            <button
              type="button"
              onClick={() => setResult(null)}
              className="text-sable-600 text-xs hover:underline"
            >
              {t('registrations.transfer.dismiss')}
            </button>
          </div>
          {result.rejected.length > 0 && (
            <ul className="mt-1 space-y-1 text-red-800">
              {result.rejected.map((r) => (
                <li key={r.registrationId}>
                  {nameOf(r.registrationId)} — {issueText(r.code, r.message)}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
