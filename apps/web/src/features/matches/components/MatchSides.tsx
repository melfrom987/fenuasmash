import { useTranslation } from 'react-i18next';
import type { Match } from '@/api/models';
import { formatTime } from '@/lib/dates';
import type { MatchLabels } from '../labels';

function Side({ label, won }: { label: { name: string; known: boolean } | null; won: boolean }) {
  const { t } = useTranslation();
  if (label === null) {
    return <span className="text-sable-500 italic">{t('matches.unknownSide')}</span>;
  }
  if (!label.known) return <span className="text-sable-600 italic">{label.name}</span>;
  return <span className={won ? 'font-semibold' : ''}>{label.name}</span>;
}

export function MatchSides({ match, labels }: { match: Match; labels: MatchLabels }) {
  const { t } = useTranslation();
  return (
    <p className="text-sm">
      <Side label={labels.side(match, 'A')} won={match.winner === 'A'} />
      <span className="text-sable-500 mx-1.5">{t('matches.versus')}</span>
      <Side label={labels.side(match, 'B')} won={match.winner === 'B'} />
    </p>
  );
}

export function MatchHeading({ match, labels }: { match: Match; labels: MatchLabels }) {
  const { t } = useTranslation();
  const round = labels.round(match);
  return (
    <p className="text-sable-600 text-xs">
      {match.number != null && (
        <span className="text-sable-800 font-medium">
          {t('schedule.labels.number', { number: match.number })} ·{' '}
        </span>
      )}
      {labels.event(match.eventId)}
      {round && ` · ${round}`}
      {match.plannedStartAt &&
        ` · ${t('schedule.labels.plannedAt', { time: formatTime(match.plannedStartAt) })}`}
    </p>
  );
}
