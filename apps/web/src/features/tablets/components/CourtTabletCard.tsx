import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Court, PairingCode, Tablet } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { formatDateTime, formatRelative } from '@/lib/dates';
import { isOnline } from '../status';
import { PairingCodePanel } from './PairingCodePanel';

export function CourtTabletCard({
  court,
  tablet,
  pairingCode,
  justPaired,
  now,
  creating,
  revoking,
  error,
  onCreateCode,
  onRevoke,
  simulating,
  onSimulate,
}: {
  court: Court;
  tablet: Tablet | undefined;
  pairingCode: PairingCode | undefined;
  justPaired: boolean;
  now: number;
  creating: boolean;
  revoking: boolean;
  error: string | null;
  onCreateCode: () => void;
  onRevoke: () => void;
  simulating: boolean;
  onSimulate?: () => void;
}) {
  const { t, i18n } = useTranslation();
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const online = tablet !== undefined && isOnline(tablet, now);
  const language = i18n.resolvedLanguage ?? 'fr';

  return (
    <li className="space-y-3 rounded-lg bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h3 className={`font-medium ${court.active ? '' : 'text-sable-500'}`}>
              {t('courts.label', { number: court.number })}
            </h3>
            {!court.active && (
              <span className="bg-sable-200 text-sable-700 rounded px-1.5 py-0.5 text-xs">
                {t('courts.inactive')}
              </span>
            )}
            {justPaired && (
              <span className="bg-lagon-100 text-lagon-800 rounded px-1.5 py-0.5 text-xs">
                {t('tablets.justPaired')}
              </span>
            )}
          </div>

          {tablet ? (
            <div className="mt-1 text-sm">
              <p className="flex items-center gap-2">
                <span
                  className={`inline-block size-2 rounded-full ${
                    online ? 'bg-lagon-500' : 'bg-sable-400'
                  }`}
                  aria-hidden
                />
                <span className="font-medium">{tablet.deviceName ?? t('tablets.unnamed')}</span>
                <span className="text-sable-600">
                  {online ? t('tablets.online') : t('tablets.offline')}
                </span>
              </p>
              <p className="text-sable-600 text-xs">
                {t('tablets.pairedAt', { date: formatDateTime(tablet.pairedAt) })} ·{' '}
                {t('tablets.lastSeen', { time: formatRelative(tablet.lastSeenAt, language, now) })}
              </p>
            </div>
          ) : (
            <p className="text-sable-600 mt-1 text-sm">{t('tablets.none')}</p>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {tablet && !confirmRevoke && (
            <button
              type="button"
              onClick={() => setConfirmRevoke(true)}
              className="text-sm text-red-700 hover:underline"
            >
              {t('tablets.revoke')}
            </button>
          )}
          {tablet && confirmRevoke && (
            <>
              <button
                type="button"
                onClick={() => setConfirmRevoke(false)}
                className="text-sable-600 text-sm hover:underline"
              >
                {t('common.cancel')}
              </button>
              <button
                type="button"
                disabled={revoking}
                onClick={() => {
                  onRevoke();
                  setConfirmRevoke(false);
                }}
                className="rounded-md bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700 disabled:opacity-50"
              >
                {t('tablets.revokeConfirm')}
              </button>
            </>
          )}
          {!pairingCode && (
            <button
              type="button"
              disabled={!court.active || creating}
              title={court.active ? undefined : t('tablets.inactiveHint')}
              onClick={onCreateCode}
              className={secondaryButtonClass}
            >
              {tablet ? t('tablets.replace') : t('tablets.pair')}
            </button>
          )}
        </div>
      </div>

      {confirmRevoke && <p className="text-sable-700 text-sm">{t('tablets.revokeHint')}</p>}
      {error && <ErrorMessage>{error}</ErrorMessage>}

      {pairingCode && (
        <PairingCodePanel
          pairingCode={pairingCode}
          renewing={creating}
          onRenew={onCreateCode}
          simulating={simulating}
          onSimulate={onSimulate}
        />
      )}
      {pairingCode && tablet && (
        <p className="text-sable-600 text-xs">{t('tablets.replaceHint')}</p>
      )}
    </li>
  );
}
