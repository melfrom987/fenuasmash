import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useOrganizations } from '@/features/organizations/api';
import { useCreateTournament } from '@/features/tournaments/api';
import { TournamentForm } from '@/features/tournaments/components/TournamentForm';
import type { TournamentFormValues } from '@/features/tournaments/schema';
import { tahitiInputToIso } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';

export function CreateTournamentPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const organizations = useOrganizations();
  const createTournament = useCreateTournament();

  // Les erreurs remontent au formulaire, qui les affiche sous le bon champ
  const handleSubmit = async (values: TournamentFormValues) => {
    await createTournament.mutateAsync({
      ...values,
      startsAt: tahitiInputToIso(values.startsAt),
      endsAt: tahitiInputToIso(values.endsAt),
    });
    await navigate('/admin');
  };

  return (
    <div className="max-w-xl">
      <Link to="/admin" className="text-lagon-700 text-sm hover:underline">
        {t('tournaments.backToList')}
      </Link>
      <h1 className="font-display mt-2 text-2xl font-semibold">{t('tournaments.create.title')}</h1>
      <p className="text-sable-600 mt-1 text-sm">{t('tournaments.create.timezoneHint')}</p>

      <div className="mt-6 space-y-4 rounded-lg bg-white p-6 shadow-sm">
        {organizations.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
        {organizations.isError && (
          <ErrorMessage>
            {t('tournaments.create.organizationsError', {
              message: errorMessage(organizations.error),
            })}
          </ErrorMessage>
        )}
        {organizations.data?.length === 0 && (
          <p className="text-sable-600">{t('tournaments.create.noOrganization')}</p>
        )}
        {organizations.data && organizations.data.length > 0 && (
          <TournamentForm organizations={organizations.data} onSubmit={handleSubmit} />
        )}
      </div>
    </div>
  );
}
