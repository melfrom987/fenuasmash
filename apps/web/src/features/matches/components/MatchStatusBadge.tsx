import { useTranslation } from 'react-i18next';
import type { MatchStatus } from '@/api/models';
import { STATUS_CLASSES } from '../status';

export function MatchStatusBadge({ status }: { status: MatchStatus }) {
  const { t } = useTranslation();
  return (
    <span className={`rounded px-1.5 py-0.5 text-xs whitespace-nowrap ${STATUS_CLASSES[status]}`}>
      {t(`matches.statuses.${status}`)}
    </span>
  );
}
