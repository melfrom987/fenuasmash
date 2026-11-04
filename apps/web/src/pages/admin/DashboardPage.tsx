import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournaments } from '@/features/tournaments/api';
import { TournamentCard } from '@/features/tournaments/components/TournamentCard';
import { Link } from 'react-router';
import { Add01Icon } from '@hugeicons/core-free-icons';
import { Icon } from '@/components/ui/Icon';

export function DashboardPage() {
  const tournaments = useTournaments();

  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <h1 className="font-display text-2xl font-semibold">Tournois</h1>
        <Link
          to="/admin/tournaments/new"
          className="bg-lagon-600 hover:bg-lagon-700 inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          <Icon icon={Add01Icon} size={18} />
          Nouveau tournoi
        </Link>
      </div>

      <div className="mt-4">
        {tournaments.isPending && <p className="text-sable-600">Chargement…</p>}
        {tournaments.isError && (
          <ErrorMessage>
            Impossible de charger les tournois : {tournaments.error.message}
          </ErrorMessage>
        )}
        {tournaments.data?.length === 0 && (
          <p className="text-sable-600">Aucun tournoi pour le moment.</p>
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
