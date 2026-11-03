import { Link, NavLink, Outlet, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournament } from '@/features/tournaments/api';
import { formatDate } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';

const tabClass = ({ isActive }: { isActive: boolean }) =>
  `-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
    isActive
      ? 'border-slate-900 text-slate-900'
      : 'border-transparent text-slate-500 hover:text-slate-700'
  }`;

export function TournamentLayout() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);

  return (
    <div>
      <Link to="/admin" className="text-sm text-blue-600 hover:underline">
        {t('tournaments.backToList')}
      </Link>

      {tournament.isPending && <p className="mt-4 text-slate-500">{t('common.loading')}</p>}
      {tournament.isError && (
        <div className="mt-4">
          <ErrorMessage>{errorMessage(tournament.error)}</ErrorMessage>
        </div>
      )}

      {tournament.data && (
        <>
          <h1 className="mt-2 text-2xl font-semibold">{tournament.data.name}</h1>
          <p className="mt-1 text-sm text-slate-500">
            {t('tournaments.dateRange', {
              start: formatDate(tournament.data.startsAt),
              end: formatDate(tournament.data.endsAt),
            })}
          </p>

          <nav className="mt-6 flex gap-1 border-b border-slate-200">
            <NavLink to="settings" className={tabClass}>
              {t('tournamentDetail.tabs.settings')}
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
