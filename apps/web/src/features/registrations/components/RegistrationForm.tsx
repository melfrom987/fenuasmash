import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Player } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useErrorMessage } from '@/lib/use-error-message';
import { useCreateRegistration } from '../api';
import { isDoubles } from '../constants';
import { PlayerPicker } from './PlayerPicker';

export function RegistrationForm({ tournamentId, event }: { tournamentId: string; event: Event }) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const createRegistration = useCreateRegistration(tournamentId, event.id);
  const doubles = isDoubles(event.discipline);

  const [player, setPlayer] = useState<Player | null>(null);
  const [partner, setPartner] = useState<Player | null>(null);
  const [lookingForPartner, setLookingForPartner] = useState(false);

  const canSubmit = player !== null && (!doubles || partner !== null || lookingForPartner);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!player) return;
    createRegistration.mutate(
      {
        playerId: player.id,
        ...(doubles && { partnerId: lookingForPartner ? null : (partner?.id ?? null) }),
      },
      {
        onSuccess: () => {
          setPlayer(null);
          setPartner(null);
          setLookingForPartner(false);
        },
      },
    );
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {createRegistration.isError && (
        <ErrorMessage>{errorMessage(createRegistration.error)}</ErrorMessage>
      )}

      <div className={`grid gap-4 ${doubles ? 'md:grid-cols-2' : ''}`}>
        <PlayerPicker
          eventId={event.id}
          label={doubles ? t('registrations.form.player1') : t('registrations.form.player')}
          value={player}
          onChange={(selected) => {
            setPlayer(selected);
            setPartner(null); // le partenaire possible dépend du joueur
            createRegistration.reset();
          }}
        />

        {doubles && (
          <div className="space-y-2">
            {player && !lookingForPartner && (
              <PlayerPicker
                eventId={event.id}
                partnerOf={player.id}
                label={t('registrations.form.partner')}
                value={partner}
                onChange={setPartner}
              />
            )}
            {!player && (
              <p className="text-sable-600 pt-6 text-sm">
                {t('registrations.form.choosePlayerFirst')}
              </p>
            )}
            {player && (
              <label className="text-sable-800 flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={lookingForPartner}
                  onChange={(e) => {
                    setLookingForPartner(e.target.checked);
                    setPartner(null);
                  }}
                  className="accent-lagon-600 size-4"
                />
                {t('registrations.form.lookingForPartner')}
              </label>
            )}
          </div>
        )}
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={!canSubmit || createRegistration.isPending}>
          {createRegistration.isPending
            ? t('registrations.form.saving')
            : t('registrations.form.submit')}
        </Button>
      </div>
    </form>
  );
}
