import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Tick02Icon } from '@hugeicons/core-free-icons';
import { Icon } from '@/components/ui/Icon';
import { STEP_PATHS, STEPS, type Progress, type StepId, type StepSummary } from '../progress';

function SummaryText({ summary }: { summary: StepSummary }) {
  const { t } = useTranslation();
  switch (summary.step) {
    case 'prepare':
      return (
        <>
          {t('tournamentDetail.summary.courts', { count: summary.courts })} ·{' '}
          {t('tournamentDetail.summary.events', { count: summary.events })}
        </>
      );
    case 'registrations':
      return <>{t('tournamentDetail.summary.entries', { count: summary.entries })}</>;
    case 'draws':
      return (
        <>
          {t('tournamentDetail.summary.draws', {
            published: summary.published,
            total: summary.total,
          })}
        </>
      );
    case 'schedule':
      return <>{t('tournamentDetail.summary.soon')}</>;
    case 'matchday':
      return summary.live > 0 ? (
        <>{t('tournamentDetail.summary.live', { count: summary.live })}</>
      ) : (
        <>{t('tournamentDetail.summary.matches', { count: summary.total })}</>
      );
  }
}

export function TournamentSteps({
  progress,
  activeStep,
}: {
  progress: Progress | null;
  activeStep: StepId;
}) {
  const { t } = useTranslation();

  return (
    <ol className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:grid md:grid-cols-5 md:px-0">
      {STEPS.map(({ id }, index) => {
        const state = progress?.states[id] ?? 'todo';
        const summary = progress?.summaries[index];
        const active = id === activeStep;
        const content = (
          <>
            <span className="flex items-center gap-2">
              <span
                className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                  state === 'done'
                    ? 'bg-lagon-600 text-white'
                    : state === 'current'
                      ? 'bg-sunset-500 text-white'
                      : 'bg-sable-200 text-sable-600'
                }`}
              >
                {state === 'done' ? <Icon icon={Tick02Icon} size={14} /> : index + 1}
              </span>
              <span className="text-sm font-medium whitespace-nowrap">
                {t(`tournamentDetail.steps.${id}`)}
              </span>
            </span>
            <span className="text-sable-600 mt-1 block truncate text-xs">
              {summary ? <SummaryText summary={summary} /> : ' '}
            </span>
          </>
        );
        const base = `block min-w-40 rounded-lg border px-3 py-2 md:min-w-0 ${
          active ? 'border-lagon-500 bg-white shadow-sm' : 'border-sable-200 bg-sable-50'
        }`;

        return (
          <li key={id} className="shrink-0 md:shrink">
            {state === 'soon' ? (
              <div className={`${base} opacity-60`} aria-disabled>
                {content}
              </div>
            ) : (
              <Link
                to={STEP_PATHS[id][0]}
                aria-current={active ? 'step' : undefined}
                className={`${base} hover:border-lagon-400`}
              >
                {content}
              </Link>
            )}
          </li>
        );
      })}
    </ol>
  );
}
