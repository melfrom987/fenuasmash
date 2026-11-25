import { useTranslation } from 'react-i18next';
import type { PairingCode } from '@/api/models';
import { useNow } from '@/lib/use-now';
import { formatCode } from '../status';

const pad = (value: number) => String(value).padStart(2, '0');

export function PairingCodePanel({
  pairingCode,
  renewing,
  onRenew,
  simulating,
  onSimulate,
}: {
  pairingCode: PairingCode;
  renewing: boolean;
  onRenew: () => void;
  simulating: boolean;
  onSimulate?: () => void;
}) {
  const { t } = useTranslation();
  const now = useNow(1000);
  const remaining = Math.max(0, Math.round((Date.parse(pairingCode.expiresAt) - now) / 1000));
  const expired = remaining === 0;

  return (
    <div
      className={`rounded-md border p-4 ${
        expired ? 'border-sable-300 bg-sable-50' : 'border-lagon-200 bg-lagon-50'
      }`}
    >
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sable-700 text-xs font-medium tracking-wide uppercase">
            {t('tablets.code.title')}
          </p>
          <p
            className={`font-mono text-3xl font-semibold tracking-[0.2em] tabular-nums ${
              expired ? 'text-sable-400 line-through' : 'text-ocean-950'
            }`}
            aria-live="polite"
          >
            {formatCode(pairingCode.code)}
          </p>
        </div>
        <div className="text-right text-sm">
          {expired ? (
            <p className="text-sunset-800 font-medium">{t('tablets.code.expired')}</p>
          ) : (
            <p className="text-sable-700 tabular-nums">
              {t('tablets.code.expiresIn', {
                time: `${Math.floor(remaining / 60)}:${pad(remaining % 60)}`,
              })}
            </p>
          )}
          <button
            type="button"
            onClick={onRenew}
            disabled={renewing}
            className="text-lagon-700 mt-1 text-sm hover:underline disabled:opacity-50"
          >
            {t('tablets.code.renew')}
          </button>
        </div>
      </div>
      <p className="text-sable-600 mt-2 text-xs">{t('tablets.code.hint')}</p>
      {onSimulate && !expired && (
        <button
          type="button"
          onClick={onSimulate}
          disabled={simulating}
          className="text-sable-600 mt-2 text-xs underline disabled:opacity-50"
        >
          {t('tablets.code.simulate')}
        </button>
      )}
    </div>
  );
}
