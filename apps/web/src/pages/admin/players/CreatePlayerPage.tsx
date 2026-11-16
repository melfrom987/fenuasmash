import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useCreatePlayer } from '@/features/players/api';
import { PlayerForm } from '@/features/players/components/PlayerForm';
import { toPlayerInput, type PlayerFormValues } from '@/features/players/schema';

export function CreatePlayerPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const createPlayer = useCreatePlayer();

  const handleSubmit = async (values: PlayerFormValues) => {
    await createPlayer.mutateAsync(toPlayerInput(values));
    await navigate('/admin/players');
  };

  return (
    <div className="max-w-6xl space-y-4">
      <Link to="/admin/players" className="text-lagon-700 text-sm hover:underline">
        {t('players.backToList')}
      </Link>
      <h1 className="font-display text-2xl font-semibold">{t('players.createTitle')}</h1>
      <PlayerForm onSubmit={handleSubmit} />
    </div>
  );
}
