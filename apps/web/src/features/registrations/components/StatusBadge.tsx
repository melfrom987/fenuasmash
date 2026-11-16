import { useTranslation } from 'react-i18next';
import type { RegistrationStatus } from '@/api/models';

const styles: Record<RegistrationStatus, string> = {
  CONFIRMED: 'bg-lagon-100 text-lagon-800',
  PENDING_PARTNER: 'bg-sunset-100 text-sunset-800',
  LOOKING_FOR_PARTNER: 'bg-hibiscus-100 text-hibiscus-800',
};

export function StatusBadge({ status }: { status: RegistrationStatus }) {
  const { t } = useTranslation();
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ${styles[status]}`}
    >
      {t(`registrations.statuses.${status}`)}
    </span>
  );
}
