import { useParams } from 'react-router';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useTournament } from '@/features/tournaments/api';

export function LivePage() {
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);

  if (tournament.isPending) return <p className="text-slate-500">Chargement…</p>;
  if (tournament.isError) return <ErrorMessage>{tournament.error.message}</ErrorMessage>;

  return (
    <div>
      <h1 className="text-2xl font-semibold">{tournament.data.name}</h1>
      <p className="mt-2 text-slate-500">Les scores en direct apparaîtront ici.</p>
    </div>
  );
}
