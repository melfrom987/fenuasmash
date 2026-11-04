import { useParams } from 'react-router';

export function DisplayPage() {
  const { tournamentId } = useParams();
  return (
    <div>
      <h1 className="font-display text-5xl font-bold">Terrains</h1>
      <p className="text-ocean-200 mt-4 text-2xl">Tournoi : {tournamentId}</p>
    </div>
  );
}
