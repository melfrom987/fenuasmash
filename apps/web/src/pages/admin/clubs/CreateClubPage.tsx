import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useCreateClub, useUploadClubLogo } from '@/features/clubs/api';
import { ClubForm } from '@/features/clubs/components/ClubForm';
import type { LogoChange } from '@/features/clubs/logo';
import { toClubInput, type ClubFormValues } from '@/features/clubs/schema';

export type ClubPageState = { logoFailed?: boolean };

export function CreateClubPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const createClub = useCreateClub();
  const uploadLogo = useUploadClubLogo();

  const handleSubmit = async (values: ClubFormValues, logo: LogoChange) => {
    const club = await createClub.mutateAsync(toClubInput(values));
    if (logo.type === 'replace') {
      try {
        await uploadLogo.mutateAsync({ clubId: club.id, file: logo.file });
      } catch {
        const state: ClubPageState = { logoFailed: true };
        await navigate(`/admin/clubs/${club.id}`, { state });
        return;
      }
    }
    await navigate('/admin/clubs');
  };

  return (
    <div className="max-w-6xl space-y-4">
      <Link to="/admin/clubs" className="text-lagon-700 text-sm hover:underline">
        {t('clubs.backToList')}
      </Link>
      <h1 className="font-display text-2xl font-semibold">{t('clubs.createTitle')}</h1>
      <ClubForm onSubmit={handleSubmit} />
    </div>
  );
}
