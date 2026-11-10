import { Link, NavLink, Outlet, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournament } from '@/features/tournaments/api';
import { formatDate } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';

const tabClass = ({ isActive }: { isActive: boolean }) =>
  `-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
    isActive
      ? 'border-lagon-600 text-lagon-700'
      : 'border-transparent text-sable-600 hover:text-sable-800'
  }`;

export function TournamentLayout() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);

  return (
    <div>
      <Link to="/admin" className="text-lagon-700 text-sm hover:underline">
        {t('tournaments.backToList')}
      </Link>

      {tournament.isPending && <p className="text-sable-600 mt-4">{t('common.loading')}</p>}
      {tournament.isError && (
        <div className="mt-4">
          <ErrorMessage>{errorMessage(tournament.error)}</ErrorMessage>
        </div>
      )}

      {tournament.data && (
        <>
          <h1 className="font-display mt-2 text-2xl font-semibold">{tournament.data.name}</h1>
          <p className="text-sable-600 mt-1 text-sm">
            {t('tournaments.dateRange', {
              start: formatDate(tournament.data.startsAt),
              end: formatDate(tournament.data.endsAt),
            })}
          </p>

          <nav className="border-sable-200 mt-6 flex gap-1 border-b">
            <NavLink to="settings" className={tabClass}>
              {t('tournamentDetail.tabs.settings')}
            </NavLink>
            <NavLink to="events" className={tabClass}>
              {t('tournamentDetail.tabs.events')}
            </NavLink>
          </nav>

          <div className="mt-6">
            <Outlet />
          </div>
        </>
      )}
    </div>
  );
}
