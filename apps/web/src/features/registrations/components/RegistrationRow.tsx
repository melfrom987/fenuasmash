import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Player, Registration } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { formatDateTime } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';
import { useDeleteRegistration, useUpdateRegistration } from '../api';
import { isDoubles } from '../constants';
import { PlayerLine, PlayerPicker } from './PlayerPicker';
import { StatusBadge } from './StatusBadge';

type Mode = 'view' | 'partner' | 'withdraw';

const linkButtonClass = 'text-lagon-700 text-sm hover:underline disabled:opacity-50';

export function RegistrationRow({
  tournamentId,
  event,
  registration,
  rank,
  points,
  outOfCategory,
  referenceYear,
  selected,
  onToggle,
  suggestion,
  switching,
  onSwitch,
}: {
  tournamentId: string;
  event: Event;
  registration: Registration;
  rank: number;
  points: number | null;
  outOfCategory: boolean;
  referenceYear: number;
  selected: boolean;
  onToggle: () => void;
  suggestion: Event | undefined;
  switching: boolean;
  onSwitch: () => void;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const updateRegistration = useUpdateRegistration(tournamentId);
  const deleteRegistration = useDeleteRegistration(tournamentId);
  const [mode, setMode] = useState<Mode>('view');
  const [newPartner, setNewPartner] = useState<Player | null>(null);
  const doubles = isDoubles(event.discipline);
  const error = updateRegistration.error ?? deleteRegistration.error;

  const changeMode = (next: Mode) => {
    setMode(next);
    setNewPartner(null);
    updateRegistration.reset();
    deleteRegistration.reset();
  };

  const setPartner = (partnerId: string | null) =>
    updateRegistration.mutate(
      { id: registration.id, input: { partnerId } },
      { onSuccess: () => changeMode('view') },
    );

  return (
    <li className={`space-y-3 px-4 py-3 ${selected ? 'bg-lagon-50' : ''}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          <input
            type="checkbox"
            checked={selected}
            onChange={onToggle}
            aria-label={t('registrations.select')}
            className="accent-lagon-600 mt-0.5 size-4 shrink-0"
          />
          <span className="text-sable-500 w-6 shrink-0 text-right text-sm tabular-nums">
            {rank}
          </span>
          <div className="min-w-0 space-y-1">
            <PlayerLine
              player={registration.player}
              clubName={registration.player.clubName}
              referenceYear={referenceYear}
            />
            {registration.partner && (
              <PlayerLine
                player={registration.partner}
                clubName={registration.partner.clubName}
                referenceYear={referenceYear}
              />
            )}
            <p className="text-sable-600 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
              <span className="tabular-nums">
                {points === null
                  ? t('registrations.unranked')
                  : t('registrations.points', { count: points })}
              </span>
              <span>
                {t('registrations.registeredOn', {
                  date: formatDateTime(registration.registeredAt),
                })}
              </span>
              {outOfCategory && (
                <span className="rounded-full bg-red-100 px-2 py-0.5 font-medium text-red-800">
                  {t('registrations.outOfCategory')}
                </span>
              )}
              {outOfCategory &&
                (suggestion ? (
                  <button
                    type="button"
                    disabled={switching}
                    onClick={onSwitch}
                    className="text-lagon-700 font-medium hover:underline disabled:opacity-50"
                  >
                    {t('registrations.switchTo', { event: suggestion.name })}
                  </button>
                ) : (
                  <span className="text-sable-600">{t('registrations.noSuitableEvent')}</span>
                ))}
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {doubles && <StatusBadge status={registration.status} />}
          {mode === 'view' && (
            <>
              {registration.status === 'PENDING_PARTNER' && (
                <button
                  type="button"
                  disabled={updateRegistration.isPending}
                  onClick={() =>
                    updateRegistration.mutate({ id: registration.id, input: { confirm: true } })
                  }
                  className={linkButtonClass}
                >
                  {t('registrations.row.confirm')}
                </button>
              )}
              {doubles && (
                <button
                  type="button"
                  onClick={() => changeMode('partner')}
                  className={linkButtonClass}
                >
                  {registration.partner
                    ? t('registrations.row.changePartner')
                    : t('registrations.row.addPartner')}
                </button>
              )}
              <button
                type="button"
                onClick={() => changeMode('withdraw')}
                className="text-sm text-red-700 hover:underline"
              >
                {t('registrations.row.withdraw')}
              </button>
            </>
          )}
        </div>
      </div>

      {error && <ErrorMessage>{errorMessage(error)}</ErrorMessage>}

      {mode === 'partner' && (
        <div className="bg-sable-50 space-y-3 rounded-md p-3">
          <PlayerPicker
            eventId={event.id}
            partnerOf={registration.player.id}
            label={t('registrations.form.partner')}
            value={newPartner}
            onChange={setNewPartner}
          />
          <div className="flex flex-wrap justify-end gap-2">
            {registration.partner && (
              <button
                type="button"
                disabled={updateRegistration.isPending}
                onClick={() => setPartner(null)}
                className="mr-auto rounded-md px-3 py-2 text-sm text-red-700 hover:bg-red-50"
              >
                {t('registrations.row.removePartner')}
              </button>
            )}
            <button
              type="button"
              onClick={() => changeMode('view')}
              className={secondaryButtonClass}
            >
              {t('common.cancel')}
            </button>
            <Button
              type="button"
              disabled={!newPartner || updateRegistration.isPending}
              onClick={() => newPartner && setPartner(newPartner.id)}
            >
              {t('common.save')}
            </Button>
          </div>
        </div>
      )}

      {mode === 'withdraw' && (
        <div className="flex flex-wrap items-center justify-end gap-2 rounded-md bg-red-50 p-3">
          <p className="mr-auto text-sm text-red-800">{t('registrations.row.withdrawConfirm')}</p>
          <button type="button" onClick={() => changeMode('view')} className={secondaryButtonClass}>
            {t('common.cancel')}
          </button>
          <button
            type="button"
            disabled={deleteRegistration.isPending}
            onClick={() => deleteRegistration.mutate(registration)}
            className="rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800 disabled:opacity-50"
          >
            {t('registrations.row.withdraw')}
          </button>
        </div>
      )}
    </li>
  );
}
