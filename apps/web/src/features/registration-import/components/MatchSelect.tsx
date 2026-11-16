import { useTranslation } from 'react-i18next';
import { inputClass } from '@/components/ui/styles';
import { formatBirthDate } from '@/lib/dates';
import { playerName } from '@/features/registrations/format';
import type { MatchResult, PlayerDecision, PlayerMatch } from '../types';

const badgeStyles: Record<MatchResult, string> = {
  EXACT: 'bg-lagon-100 text-lagon-800',
  PROBABLE: 'bg-sunset-100 text-sunset-800',
  AMBIGUOUS: 'bg-hibiscus-100 text-hibiscus-800',
  NONE: 'bg-sable-100 text-sable-800',
};

export function MatchBadge({ result }: { result: MatchResult }) {
  const { t } = useTranslation();
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ${badgeStyles[result]}`}
    >
      {t(`registrationImport.matches.${result}`)}
    </span>
  );
}

const NEW = 'NEW';
const UNDECIDED = '';

export function MatchSelect({
  match,
  decision,
  allowNew,
  onChange,
}: {
  match: PlayerMatch;
  decision: PlayerDecision | undefined;
  allowNew: boolean;
  onChange: (decision: PlayerDecision | undefined) => void;
}) {
  const { t } = useTranslation();
  if (match.result === 'EXACT' || (match.candidates.length === 0 && !allowNew)) return null;

  const value = decision
    ? decision.action === 'CREATE_NEW'
      ? NEW
      : decision.playerId
    : (match.selectedPlayerId ?? (match.result === 'NONE' ? NEW : UNDECIDED));

  return (
    <select
      value={value}
      onChange={(e) => {
        const selected = e.target.value;
        if (selected === UNDECIDED) onChange(undefined);
        else if (selected === NEW) onChange({ action: 'CREATE_NEW' });
        else onChange({ action: 'USE_EXISTING', playerId: selected });
      }}
      className={`${inputClass} mt-1 text-xs ${value === UNDECIDED ? 'border-hibiscus-400' : ''}`}
    >
      <option value={UNDECIDED} disabled>
        {t('registrationImport.preview.choosePlayer')}
      </option>
      {match.candidates.map(({ player }) => (
        <option key={player.id} value={player.id}>
          {`${playerName(player)} · ${formatBirthDate(player.birthDate)} · ${player.clubName}`}
        </option>
      ))}
      {allowNew && <option value={NEW}>{t('registrationImport.preview.newPlayer')}</option>}
    </select>
  );
}
