import { useTranslation } from 'react-i18next';
import type { Match, ScheduleWarning } from '@/api/models';
import type { MatchLabels } from '@/features/matches/labels';

export function ScheduleWarnings({
  warnings,
  matches,
  labels,
}: {
  warnings: ScheduleWarning[];
  matches: Match[];
  labels: MatchLabels;
}) {
  const { t } = useTranslation();
  if (warnings.length === 0) return null;
  const number = (matchId: string) => matches.find((m) => m.id === matchId)?.number ?? '?';

  const text = (warning: ScheduleWarning) => {
    switch (warning.code) {
      case 'NO_SLOT':
        return t('schedule.warnings.NO_SLOT', { event: labels.event(warning.eventId) });
      case 'OUTSIDE_SLOT':
        return t('schedule.warnings.OUTSIDE_SLOT', {
          event: labels.event(warning.eventId),
          count: warning.count,
        });
      case 'REST_TOO_SHORT':
        return t('schedule.warnings.REST_TOO_SHORT', { number: number(warning.matchId) });
      case 'ORDER':
        return t('schedule.warnings.ORDER', { number: number(warning.matchId) });
      case 'UNSCHEDULED':
        return t('schedule.warnings.UNSCHEDULED', { count: warning.count });
    }
  };

  return (
    <section className="border-sunset-200 bg-sunset-50 space-y-1 rounded-md border px-4 py-3">
      <h3 className="text-sunset-800 text-sm font-semibold">
        {t('schedule.warningsTitle', { count: warnings.length })}
      </h3>
      <ul className="text-sunset-900 list-disc space-y-0.5 pl-5 text-sm">
        {warnings.map((warning, index) => (
          <li key={index}>{text(warning)}</li>
        ))}
      </ul>
    </section>
  );
}
