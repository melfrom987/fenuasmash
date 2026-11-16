import { useState } from 'react';
import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Add01Icon } from '@hugeicons/core-free-icons';
import type { Gender } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Icon } from '@/components/ui/Icon';
import { Pagination } from '@/components/ui/Pagination';
import { inputClass } from '@/components/ui/styles';
import { useClubs } from '@/features/clubs/api';
import { usePlayers } from '@/features/players/api';
import { PlayerTable } from '@/features/players/components/PlayerTable';
import { GENDERS, PLAYERS_PAGE_SIZE } from '@/features/players/constants';
import { useDebouncedValue } from '@/lib/use-debounced-value';
import { useErrorMessage } from '@/lib/use-error-message';

export function PlayersPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const clubs = useClubs();

  const [search, setSearch] = useState('');
  const [clubId, setClubId] = useState('');
  const [gender, setGender] = useState<Gender | ''>('');
  const [page, setPage] = useState(0);

  // On attend que l'utilisateur ait fini de taper avant d'interroger l'API
  const q = useDebouncedValue(search.trim());
  const players = usePlayers({ q, clubId, gender, page });

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-2xl font-semibold">{t('players.title')}</h1>
        <Link
          to="new"
          className="bg-lagon-600 hover:bg-lagon-700 inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          <Icon icon={Add01Icon} size={18} />
          {t('players.new')}
        </Link>
      </div>

      <div className="grid gap-3 rounded-lg bg-white p-4 shadow-sm sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)]">
        <label className="block">
          <span className="text-sable-800 text-sm font-medium">{t('players.filters.search')}</span>
          <input
            type="search"
            value={search}
            placeholder={t('players.filters.searchPlaceholder')}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className={inputClass}
          />
        </label>
        <label className="block">
          <span className="text-sable-800 text-sm font-medium">{t('players.filters.club')}</span>
          <select
            value={clubId}
            onChange={(e) => {
              setClubId(e.target.value);
              setPage(0);
            }}
            className={inputClass}
          >
            <option value="">{t('players.filters.allClubs')}</option>
            {clubs.data?.map((club) => (
              <option key={club.id} value={club.id}>
                {club.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="text-sable-800 text-sm font-medium">{t('players.filters.gender')}</span>
          <select
            value={gender}
            onChange={(e) => {
              setGender(e.target.value as Gender | '');
              setPage(0);
            }}
            className={inputClass}
          >
            <option value="">{t('players.filters.allGenders')}</option>
            {GENDERS.map((g) => (
              <option key={g} value={g}>
                {t(`players.genders.${g}`)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {players.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {players.isError && <ErrorMessage>{errorMessage(players.error)}</ErrorMessage>}

      {players.data && (
        <div className={`space-y-3 ${players.isPlaceholderData ? 'opacity-60' : ''}`}>
          <p className="text-sable-600 text-sm">
            {t('players.count', { count: players.data.totalItems })}
          </p>
          {players.data.items.length === 0 ? (
            <p className="text-sable-600 rounded-lg bg-white p-6 text-center shadow-sm">
              {q || clubId || gender ? t('players.noResults') : t('players.empty')}
            </p>
          ) : (
            <PlayerTable players={players.data.items} />
          )}
          <Pagination
            page={players.data.page}
            size={PLAYERS_PAGE_SIZE}
            totalItems={players.data.totalItems}
            onChange={setPage}
          />
        </div>
      )}
    </div>
  );
}
