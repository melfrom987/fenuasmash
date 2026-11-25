import { Link, NavLink, Outlet, useLocation, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournament } from '@/features/tournaments/api';
import { NextStepBanner } from '@/features/tournaments/components/NextStepBanner';
import { TournamentSteps } from '@/features/tournaments/components/TournamentSteps';
import { STEPS, stepOf, type NextAction } from '@/features/tournaments/progress';
import { useTournamentProgress } from '@/features/tournaments/use-tournament-progress';
import { formatDate } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';

const tabClass = ({ isActive }: { isActive: boolean }) =>
  `-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
    isActive
      ? 'border-lagon-600 text-lagon-700'
      : 'border-transparent text-sable-600 hover:text-sable-800'
  }`;

function isHere(next: NextAction, path: string, search: string) {
  const [targetPath, targetSearch = ''] = next.to.split('?');
  return path === targetPath && (!targetSearch || search === `?${targetSearch}`);
}

export function TournamentLayout() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const location = useLocation();
  const tournament = useTournament(tournamentId);
  const progress = useTournamentProgress(tournamentId);

  const base = `/admin/tournaments/${tournamentId}/`;
  const path = location.pathname.startsWith(base) ? location.pathname.slice(base.length) : '';
  const activeStep = stepOf(path);
  const pages = STEPS.find((step) => step.id === activeStep)?.pages ?? [];

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

          <nav aria-label={t('tournamentDetail.stepsLabel')} className="mt-6">
            <TournamentSteps progress={progress} activeStep={activeStep} />
          </nav>

          {progress?.next && (
            <div className="mt-3">
              <NextStepBanner
                next={progress.next}
                here={isHere(progress.next, path, location.search)}
              />
            </div>
          )}

          {pages.length > 1 && (
            <nav className="border-sable-200 mt-6 flex gap-1 border-b">
              {pages.map((page) => (
                <NavLink key={page.path} to={page.path} className={tabClass}>
                  {t(`tournamentDetail.tabs.${page.label}`)}
                </NavLink>
              ))}
            </nav>
          )}

          <div className="mt-6">
            <Outlet />
          </div>
        </>
      )}
    </div>
  );
}
