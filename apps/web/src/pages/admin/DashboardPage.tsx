import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournaments } from '@/features/tournaments/api';
import { TournamentCard } from '@/features/tournaments/components/TournamentCard';
import { Link } from 'react-router';

export function DashboardPage() {
  const tournaments = useTournaments();

  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Tournois</h1>
        <Link
          to="/admin/tournaments/new"
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Nouveau tournoi
        </Link>
      </div>

      <div className="mt-4">
        {tournaments.isPending && <p className="text-slate-500">Chargement…</p>}
        {tournaments.isError && (
          <ErrorMessage>
            Impossible de charger les tournois : {tournaments.error.message}
          </ErrorMessage>
        )}
        {tournaments.data?.length === 0 && (
          <p className="text-slate-500">Aucun tournoi pour le moment.</p>
        )}
        {tournaments.data && tournaments.data.length > 0 && (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4">
            {tournaments.data.map((t) => (
              <TournamentCard key={t.id} tournament={t} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
