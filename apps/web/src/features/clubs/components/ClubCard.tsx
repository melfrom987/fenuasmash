import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Club } from '@/api/models';
import { ClubLogo } from './ClubLogo';

export function ClubCard({ club }: { club: Club }) {
  const { t } = useTranslation();

  return (
    <li className="flex gap-4 rounded-lg bg-white p-4 shadow-sm">
      <ClubLogo club={club} />
      <div className="min-w-0 flex-1">
        <p className="font-display truncate font-semibold">{club.name}</p>
        <p className="text-sable-600 text-sm">{club.city}</p>
        {(club.email || club.phone) && (
          <p className="text-sable-600 mt-1 truncate text-xs">
            {[club.email, club.phone].filter(Boolean).join(' · ')}
          </p>
        )}
      </div>
      <Link to={club.id} className="text-lagon-700 self-start text-sm hover:underline">
        {t('clubs.edit')}
      </Link>
    </li>
  );
}
