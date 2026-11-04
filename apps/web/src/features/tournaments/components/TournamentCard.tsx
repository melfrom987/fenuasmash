import { Link } from 'react-router';
import {
  Calendar03Icon,
  LiveStreaming02Icon,
  Settings02Icon,
  Tv01Icon,
} from '@hugeicons/core-free-icons';
import type { Tournament } from '@/api/models';
import { Icon } from '@/components/ui/Icon';
import { formatDate } from '@/lib/dates';
import { t } from 'i18next';

const linkClass = 'inline-flex items-center gap-1 text-lagon-700 hover:underline';

export function TournamentCard({ tournament }: { tournament: Tournament }) {
  return (
    <article className="border-sable-200 rounded-lg border bg-white p-4 shadow-sm">
      <h2 className="font-display font-semibold">{tournament.name}</h2>
      <p className="text-sable-600 mt-1 flex items-center gap-1.5 text-sm">
        <Icon icon={Calendar03Icon} size={16} />
        Du {formatDate(tournament.startsAt)} au {formatDate(tournament.endsAt)}
      </p>
      <div className="mt-3 flex flex-wrap gap-3 text-sm">
        <Link to={`/admin/tournaments/${tournament.id}`} className={`${linkClass} font-medium`}>
          <Icon icon={Settings02Icon} size={16} />
          {t('tournaments.manage')}
        </Link>
        <Link to={`/live/${tournament.id}`} className={linkClass}>
          <Icon icon={LiveStreaming02Icon} size={16} />
          Page live
        </Link>
        <Link to={`/display/${tournament.id}`} className={linkClass}>
          <Icon icon={Tv01Icon} size={16} />
          Écran TV
        </Link>
      </div>
    </article>
  );
}
