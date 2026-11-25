import { useTranslation } from 'react-i18next';
import type { Tablet } from '@/api/models';
import { formatDateTime } from '@/lib/dates';

export function TabletHistory({ tablets }: { tablets: Tablet[] }) {
  const { t } = useTranslation();
  if (tablets.length === 0) return null;

  return (
    <details className="rounded-lg bg-white shadow-sm">
      <summary className="cursor-pointer px-4 py-3 text-sm font-medium">
        {t('tablets.history', { count: tablets.length })}
      </summary>
      <ul className="divide-sable-200 border-sable-200 divide-y border-t">
        {tablets.map((tablet) => (
          <li key={tablet.id} className="flex flex-wrap justify-between gap-2 px-4 py-2 text-sm">
            <span>
              {t('courts.label', { number: tablet.courtNumber })} ·{' '}
              {tablet.deviceName ?? t('tablets.unnamed')}
            </span>
            <span className="text-sable-600 text-xs">
              {t('tablets.pairedAt', { date: formatDateTime(tablet.pairedAt) })} ·{' '}
              {t('tablets.revokedAt', {
                date: formatDateTime(tablet.revokedAt ?? tablet.pairedAt),
              })}
            </span>
          </li>
        ))}
      </ul>
    </details>
  );
}
