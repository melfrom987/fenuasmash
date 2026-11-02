import { Link } from 'react-router';
import type { Tournament } from '@/api/models';
import { formatDate } from '@/lib/dates';

export function TournamentCard({ tournament }: { tournament: Tournament }) {
  return (
    <article className="rounded-lg bg-white p-4 shadow-sm">
      <h2 className="font-semibold">{tournament.name}</h2>
      <p className="mt-1 text-sm text-slate-500">
        Du {formatDate(tournament.startsAt)} au {formatDate(tournament.endsAt)}
      </p>
      <div className="mt-3 flex gap-3 text-sm">
        <Link to={`/live/${tournament.id}`} className="text-blue-600 hover:underline">
          Page live
        </Link>
        <Link to={`/display/${tournament.id}`} className="text-blue-600 hover:underline">
          Écran TV
        </Link>
      </div>
    </article>
  );
}
