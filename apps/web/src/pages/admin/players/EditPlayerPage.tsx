import { Link, useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { usePlayer, useUpdatePlayer } from '@/features/players/api';
import { PlayerForm } from '@/features/players/components/PlayerForm';
import {
  toPlayerFormValues,
  toPlayerInput,
  type PlayerFormValues,
} from '@/features/players/schema';
import { useErrorMessage } from '@/lib/use-error-message';

export function EditPlayerPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const { playerId = '' } = useParams();
  const player = usePlayer(playerId);
  const updatePlayer = useUpdatePlayer(playerId);

  const handleSubmit = async (values: PlayerFormValues) => {
    await updatePlayer.mutateAsync(toPlayerInput(values));
    await navigate('/admin/players');
  };

  return (
    <div className="max-w-6xl space-y-4">
      <Link to="/admin/players" className="text-lagon-700 text-sm hover:underline">
        {t('players.backToList')}
      </Link>

      {player.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {player.isError && <ErrorMessage>{errorMessage(player.error)}</ErrorMessage>}

      {player.data && (
        <>
          <h1 className="font-display text-2xl font-semibold">
            {t('players.editTitle', {
              name: `${player.data.firstName} ${player.data.lastName}`,
            })}
          </h1>
          {/* key : réinitialise le formulaire si on passe d'un joueur à un autre */}
          <PlayerForm
            key={player.data.id}
            defaultValues={toPlayerFormValues(player.data)}
            onSubmit={handleSubmit}
          />
        </>
      )}
    </div>
  );
}
