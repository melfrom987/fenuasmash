import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useCourts } from '@/features/courts/api';
import { CourtCountControl } from '@/features/courts/components/CourtCountControl';
import { CourtList } from '@/features/courts/components/CourtList';
import { useErrorMessage } from '@/lib/use-error-message';

export function SettingsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const courts = useCourts(tournamentId);

  return (
    <div className="max-w-2xl space-y-8">
      <section className="space-y-4">
        <div>
          <h2 className="font-display text-lg font-semibold">{t('courts.title')}</h2>
          <p className="text-sable-600 text-sm">{t('courts.hint')}</p>
        </div>

        {courts.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
        {courts.isError && <ErrorMessage>{errorMessage(courts.error)}</ErrorMessage>}
        {courts.data && (
          <>
            <CourtCountControl tournamentId={tournamentId} courts={courts.data} />
            {courts.data.length > 0 && (
              <CourtList tournamentId={tournamentId} courts={courts.data} />
            )}
          </>
        )}
      </section>
    </div>
  );
}
