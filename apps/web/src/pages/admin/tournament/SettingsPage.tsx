import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useCourts } from '@/features/courts/api';
import { CourtCountControl } from '@/features/courts/components/CourtCountControl';
import { CourtList } from '@/features/courts/components/CourtList';
import { useErrorMessage } from '@/lib/use-error-message';
import { useTournament } from '@/features/tournaments/api';
import { RulesForm } from '@/features/tournaments/components/RulesForm';

export function SettingsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const courts = useCourts(tournamentId);
  const tournament = useTournament(tournamentId);

  return (
    <div className="max-w-2xl space-y-8">
      <section className="space-y-4">
        <div>
          <h2 className="font-display text-lg font-semibold">{t('rules.title')}</h2>
          <p className="text-sable-600 text-sm">{t('rules.settingsHint')}</p>
        </div>
        {tournament.data && (
          <div className="rounded-lg bg-white p-6 shadow-sm">
            <RulesForm tournament={tournament.data} />
          </div>
        )}
      </section>
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
