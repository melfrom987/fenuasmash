import { useTranslation } from 'react-i18next';
import type { Match } from '@/api/models';
import type { MatchLabels } from '../labels';

function Side({ name, won }: { name: string | null; won: boolean }) {
  const { t } = useTranslation();
  if (name === null)
    return <span className="text-sable-500 italic">{t('matches.unknownSide')}</span>;
  return <span className={won ? 'font-semibold' : ''}>{name}</span>;
}

export function MatchSides({ match, labels }: { match: Match; labels: MatchLabels }) {
  const { t } = useTranslation();
  return (
    <p className="text-sm">
      <Side name={labels.entry(match.sideA)} won={match.winner === 'A'} />
      <span className="text-sable-500 mx-1.5">{t('matches.versus')}</span>
      <Side name={labels.entry(match.sideB)} won={match.winner === 'B'} />
    </p>
  );
}

export function MatchHeading({ match, labels }: { match: Match; labels: MatchLabels }) {
  const { t } = useTranslation();
  return (
    <p className="text-sable-600 text-xs">
      {labels.event(match.eventId)}
      {match.round !== null && ` · ${t('matches.round', { round: match.round })}`}
    </p>
  );
}
