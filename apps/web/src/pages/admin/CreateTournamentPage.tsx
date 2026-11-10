import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useOrganizations } from '@/features/organizations/api';
import { useCreateTournament } from '@/features/tournaments/api';
import { TournamentForm } from '@/features/tournaments/components/TournamentForm';
import type { TournamentFormValues } from '@/features/tournaments/schema';
import { tahitiInputToIso } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';
import { toRulesInput } from '@/features/tournaments/rules';

export function CreateTournamentPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const organizations = useOrganizations();
  const createTournament = useCreateTournament();

  const handleSubmit = async (values: TournamentFormValues) => {
    const { rules, startsAt, endsAt, ...rest } = values;
    await createTournament.mutateAsync({
      ...rest,
      startsAt: tahitiInputToIso(startsAt),
      endsAt: tahitiInputToIso(endsAt),
      ...toRulesInput(rules),
    });
    await navigate('/admin');
  };

  return (
    <div className="max-w-6xl">
      <Link to="/admin" className="text-lagon-700 text-sm hover:underline">
        {t('tournaments.backToList')}
      </Link>
      <h1 className="font-display mt-2 text-2xl font-semibold">{t('tournaments.create.title')}</h1>
      <p className="text-sable-600 mt-1 text-sm">{t('tournaments.create.timezoneHint')}</p>

      <div className="mt-6">
        {organizations.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
        {organizations.isError && (
          <ErrorMessage>
            {t('tournaments.create.organizationsError', {
              message: errorMessage(organizations.error),
            })}
          </ErrorMessage>
        )}
        {organizations.data?.length === 0 && (
          <p className="text-sable-600 rounded-lg bg-white p-6 shadow-sm">
            {t('tournaments.create.noOrganization')}
          </p>
        )}
        {organizations.data && organizations.data.length > 0 && (
          <TournamentForm organizations={organizations.data} onSubmit={handleSubmit} />
        )}
      </div>
    </div>
  );
}
