import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useClub, useDeleteClubLogo, useUpdateClub, useUploadClubLogo } from '@/features/clubs/api';
import { ClubForm } from '@/features/clubs/components/ClubForm';
import type { LogoChange } from '@/features/clubs/logo';
import { toClubFormValues, toClubInput, type ClubFormValues } from '@/features/clubs/schema';
import { useErrorMessage } from '@/lib/use-error-message';
import type { ClubPageState } from './CreateClubPage';

export function EditClubPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const location = useLocation();
  const { clubId = '' } = useParams();
  const club = useClub(clubId);
  const updateClub = useUpdateClub(clubId);
  const uploadLogo = useUploadClubLogo();
  const deleteLogo = useDeleteClubLogo();
  const logoFailed = (location.state as ClubPageState | null)?.logoFailed === true;

  const handleSubmit = async (values: ClubFormValues, logo: LogoChange) => {
    await updateClub.mutateAsync(toClubInput(values));
    if (logo.type === 'replace') await uploadLogo.mutateAsync({ clubId, file: logo.file });
    if (logo.type === 'remove') await deleteLogo.mutateAsync(clubId);
    await navigate('/admin/clubs');
  };

  return (
    <div className="max-w-6xl space-y-4">
      <Link to="/admin/clubs" className="text-lagon-700 text-sm hover:underline">
        {t('clubs.backToList')}
      </Link>

      {logoFailed && <ErrorMessage>{t('clubs.logoUploadFailed')}</ErrorMessage>}
      {club.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {club.isError && <ErrorMessage>{errorMessage(club.error)}</ErrorMessage>}

      {club.data && (
        <>
          <h1 className="font-display text-2xl font-semibold">
            {t('clubs.editTitle', { name: club.data.name })}
          </h1>
          <ClubForm
            key={club.data.id}
            defaultValues={toClubFormValues(club.data)}
            currentLogoUrl={club.data.logoUrl}
            onSubmit={handleSubmit}
          />
        </>
      )}
    </div>
  );
}
