import { useParams } from 'react-router';

export function DisplayPage() {
  const { tournamentId } = useParams();
  return (
    <div>
      <h1 className="text-5xl font-bold">Terrains</h1>
      <p className="mt-4 text-2xl text-slate-400">Tournoi : {tournamentId}</p>
    </div>
  );
}
