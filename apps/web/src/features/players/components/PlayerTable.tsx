import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Player } from '@/api/models';
import { ClubLogo } from '@/features/clubs/components/ClubLogo';
import { formatBirthDate } from '@/lib/dates';
import { AgeCategoryBadge } from './AgeCategoryBadge';

const headerClass =
  'px-4 py-2 text-left text-xs font-medium uppercase tracking-wide text-sable-600';
const cellClass = 'px-4 py-3 text-sm';

export function PlayerTable({ players }: { players: Player[] }) {
  const { t } = useTranslation();

  return (
    <div className="overflow-x-auto rounded-lg bg-white shadow-sm">
      <table className="divide-sable-200 min-w-full divide-y">
        <thead>
          <tr>
            <th className={headerClass}>{t('players.columns.name')}</th>
            <th className={headerClass}>{t('players.columns.gender')}</th>
            <th className={headerClass}>{t('players.columns.birthDate')}</th>
            <th className={headerClass}>{t('players.columns.category')}</th>
            <th className={headerClass}>{t('players.columns.club')}</th>
            <th className={headerClass}>{t('players.columns.license')}</th>
            <th className={headerClass}>
              <span className="sr-only">{t('players.edit')}</span>
            </th>
          </tr>
        </thead>
        <tbody className="divide-sable-200 divide-y">
          {players.map((player) => (
            <tr key={player.id} className="hover:bg-sable-50">
              <td className={`${cellClass} font-medium`}>
                <span className="uppercase">{player.lastName}</span> {player.firstName}
              </td>
              <td className={cellClass}>{t(`players.genders.${player.gender}`)}</td>
              <td className={`${cellClass} tabular-nums`}>{formatBirthDate(player.birthDate)}</td>
              <td className={cellClass}>
                <AgeCategoryBadge birthDate={player.birthDate} />
              </td>
              <td className={cellClass}>
                <span className="flex items-center gap-2">
                  <ClubLogo club={player.club} size="sm" />
                  {player.club.name}
                </span>
              </td>
              <td className={`${cellClass} text-sable-600 tabular-nums`}>
                {player.licenseNumber ?? '—'}
              </td>
              <td className={`${cellClass} text-right`}>
                <Link to={player.id} className="text-lagon-700 hover:underline">
                  {t('players.edit')}
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
