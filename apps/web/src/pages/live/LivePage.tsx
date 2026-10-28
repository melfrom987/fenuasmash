import { useParams } from 'react-router';

export function LivePage() {
  const { tournamentId } = useParams();
  return (
    <div>
      <h1 className="text-2xl font-semibold">Scores en direct</h1>
      <p className="mt-2 text-slate-500">Tournoi : {tournamentId}</p>
    </div>
  );
}
