import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Add01Icon } from '@hugeicons/core-free-icons';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Icon } from '@/components/ui/Icon';
import { useClubs } from '@/features/clubs/api';
import { ClubCard } from '@/features/clubs/components/ClubCard';
import { useErrorMessage } from '@/lib/use-error-message';

export function ClubsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const clubs = useClubs();

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold">{t('clubs.title')}</h1>
          <p className="text-sable-600 mt-1 text-sm">{t('clubs.hint')}</p>
        </div>
        <Link
          to="new"
          className="bg-lagon-600 hover:bg-lagon-700 inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          <Icon icon={Add01Icon} size={18} />
          {t('clubs.new')}
        </Link>
      </div>

      {clubs.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {clubs.isError && <ErrorMessage>{errorMessage(clubs.error)}</ErrorMessage>}
      {clubs.data?.length === 0 && (
        <p className="text-sable-600 rounded-lg bg-white p-6 text-center shadow-sm">
          {t('clubs.empty')}
        </p>
      )}
      {clubs.data && clubs.data.length > 0 && (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {clubs.data.map((club) => (
            <ClubCard key={club.id} club={club} />
          ))}
        </ul>
      )}
    </div>
  );
}
