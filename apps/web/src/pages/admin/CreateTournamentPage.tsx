import { Link, useNavigate } from 'react-router';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useOrganizations } from '@/features/organizations/api';
import { useCreateTournament } from '@/features/tournaments/api';
import { TournamentForm } from '@/features/tournaments/components/TournamentForm';
import type { TournamentFormValues } from '@/features/tournaments/schema';
import { tahitiInputToIso } from '@/lib/dates';

export function CreateTournamentPage() {
  const navigate = useNavigate();
  const organizations = useOrganizations();
  const createTournament = useCreateTournament();

  const handleSubmit = (values: TournamentFormValues) => {
    createTournament.mutate(
      {
        ...values,
        startsAt: tahitiInputToIso(values.startsAt),
        endsAt: tahitiInputToIso(values.endsAt),
      },
      { onSuccess: () => navigate('/admin') },
    );
  };

  return (
    <div className="max-w-xl">
      <Link to="/admin" className="text-sm text-blue-600 hover:underline">
        ← Retour aux tournois
      </Link>
      <h1 className="mt-2 text-2xl font-semibold">Nouveau tournoi</h1>
      <p className="mt-1 text-sm text-slate-500">Les horaires sont en heure de Tahiti.</p>

      <div className="mt-6 space-y-4 rounded-lg bg-white p-6 shadow-sm">
        {createTournament.isError && <ErrorMessage>{createTournament.error.message}</ErrorMessage>}
        {organizations.isPending && <p className="text-slate-500">Chargement…</p>}
        {organizations.isError && (
          <ErrorMessage>Organisations : {organizations.error.message}</ErrorMessage>
        )}
        {organizations.data && (
          <TournamentForm
            organizations={organizations.data}
            onSubmit={handleSubmit}
            submitting={createTournament.isPending}
          />
        )}
      </div>
    </div>
  );
}
