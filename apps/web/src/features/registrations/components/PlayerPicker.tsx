import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Player } from '@/api/models';
import { secondaryButtonClass, inputClass } from '@/components/ui/styles';
import { AgeCategoryBadge } from '@/features/players/components/AgeCategoryBadge';
import { useDebouncedValue } from '@/lib/use-debounced-value';
import { useEligiblePlayers } from '../api';
import { playerName } from '../format';

export function PlayerPicker({
  eventId,
  partnerOf,
  label,
  value,
  onChange,
}: {
  eventId: string;
  partnerOf?: string;
  label: string;
  value: Player | null;
  onChange: (player: Player | null) => void;
}) {
  const { t } = useTranslation();
  const inputId = useId();
  const [search, setSearch] = useState('');
  const q = useDebouncedValue(search.trim());
  const players = useEligiblePlayers(eventId, { q, partnerOf });

  if (value) {
    return (
      <div>
        <p className="text-sable-800 text-sm font-medium">{label}</p>
        <div className="border-lagon-200 bg-lagon-50 mt-1 flex items-center justify-between gap-3 rounded-md border px-3 py-2">
          <PlayerLine player={value} clubName={value.club.name} />
          <button type="button" onClick={() => onChange(null)} className={secondaryButtonClass}>
            {t('registrations.picker.change')}
          </button>
        </div>
      </div>
    );
  }

  const items = players.data?.items ?? [];
  const hasMore = (players.data?.totalItems ?? 0) > items.length;

  return (
    <div>
      <label htmlFor={inputId} className="text-sable-800 text-sm font-medium">
        {label}
      </label>
      <input
        id={inputId}
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder={t('registrations.picker.searchPlaceholder')}
        className={inputClass}
        autoComplete="off"
      />
      <div className={`mt-2 ${players.isPlaceholderData ? 'opacity-60' : ''}`}>
        {players.isPending && <p className="text-sable-600 text-sm">{t('common.loading')}</p>}
        {players.data && items.length === 0 && (
          <p className="text-sable-600 text-sm">{t('registrations.picker.noPlayer')}</p>
        )}
        {items.length > 0 && (
          <ul className="border-sable-200 divide-sable-200 divide-y rounded-md border">
            {items.map((player) => (
              <li key={player.id}>
                <button
                  type="button"
                  onClick={() => onChange(player)}
                  className="hover:bg-lagon-50 w-full px-3 py-2 text-left"
                >
                  <PlayerLine player={player} clubName={player.club.name} />
                </button>
              </li>
            ))}
          </ul>
        )}
        {hasMore && (
          <p className="text-sable-600 mt-1 text-xs">{t('registrations.picker.refine')}</p>
        )}
      </div>
    </div>
  );
}

export function PlayerLine({
  player,
  clubName,
  referenceYear,
}: {
  player: Pick<Player, 'firstName' | 'lastName' | 'birthDate'>;
  clubName: string;
  referenceYear?: number;
}) {
  return (
    <span className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm">
      <span className="font-medium">{playerName(player)}</span>
      <span className="text-sable-600">{clubName}</span>
      <AgeCategoryBadge birthDate={player.birthDate} referenceYear={referenceYear} />
      <span className="text-sable-500 text-xs tabular-nums">{player.birthDate.slice(0, 4)}</span>
    </span>
  );
}
