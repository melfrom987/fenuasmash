import { useParams } from 'react-router';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournament } from '@/features/tournaments/api';

export function LivePage() {
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);

  if (tournament.isPending) return <p className="text-sable-600">Chargement…</p>;
  if (tournament.isError) return <ErrorMessage>{tournament.error.message}</ErrorMessage>;

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">{tournament.data.name}</h1>
      <p className="text-sable-600 mt-2">Les scores en direct apparaîtront ici.</p>
    </div>
  );
}
