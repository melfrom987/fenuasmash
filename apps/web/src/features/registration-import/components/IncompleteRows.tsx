import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Gender, Player } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { usePlayers } from '@/features/players/api';
import { GENDERS } from '@/features/players/constants';
import { formatBirthDate } from '@/lib/dates';
import { useDebouncedValue } from '@/lib/use-debounced-value';
import type { RowFix, RowProblem } from '../build-rows';

const SEARCH_RESULTS = 5;

const longestWord = (text: string) =>
  text
    .split(/[\s\-–—_.,]+/)
    .filter(Boolean)
    .sort((a, b) => b.length - a.length)
    .at(0) ?? '';

function PlayerSearch({
  initialQuery,
  onChoose,
}: {
  initialQuery: string;
  onChoose: (player: Player) => void;
}) {
  const { t } = useTranslation();
  const [search, setSearch] = useState(initialQuery);
  const q = useDebouncedValue(search.trim());
  const players = usePlayers({ q, clubId: '', gender: '', page: 0 });
  const results = q ? (players.data?.items ?? []).slice(0, SEARCH_RESULTS) : [];

  return (
    <div className="space-y-2">
      <p className="text-sable-800 text-sm font-medium">
        {t('registrationImport.incomplete.find')}
      </p>
      <input
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder={t('registrationImport.incomplete.searchPlaceholder')}
        className={`${inputClass} mt-0`}
      />
      {q && players.data && results.length === 0 && (
        <p className="text-sable-600 text-sm">{t('registrationImport.incomplete.noMatch')}</p>
      )}
      {results.length > 0 && (
        <ul className="border-sable-200 divide-sable-200 divide-y rounded-md border">
          {results.map((player) => (
            <li key={player.id} className="flex items-center justify-between gap-2 px-3 py-2">
              <span className="min-w-0 text-sm">
                <span className="font-medium">
                  <span className="uppercase">{player.lastName}</span> {player.firstName}
                </span>
                <span className="text-sable-600 block text-xs">
                  {formatBirthDate(player.birthDate)} · {player.club.name}
                </span>
              </span>
              <button
                type="button"
                onClick={() => onChoose(player)}
                className="text-lagon-700 shrink-0 text-sm font-medium hover:underline"
              >
                {t('registrationImport.incomplete.choose')}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function CompleteForm({
  problem,
  onValidate,
}: {
  problem: RowProblem;
  onValidate: (fix: RowFix) => void;
}) {
  const { t } = useTranslation();
  const { draft } = problem;
  const [lastName, setLastName] = useState(draft.lastName);
  const [firstName, setFirstName] = useState(draft.firstName);
  const [birthDate, setBirthDate] = useState(draft.birthDate ?? '');
  const [gender, setGender] = useState<Gender | ''>(draft.gender ?? '');
  const [clubName, setClubName] = useState(draft.clubName);

  const valid =
    lastName.trim() !== '' &&
    firstName.trim() !== '' &&
    /^\d{4}-\d{2}-\d{2}$/.test(birthDate) &&
    gender !== '' &&
    clubName.trim() !== '';

  return (
    <div className="space-y-2">
      <p className="text-sable-800 text-sm font-medium">
        {t('registrationImport.incomplete.complete')}
      </p>
      <div className="grid gap-2 sm:grid-cols-2">
        <label className="block">
          <span className="text-sable-700 text-xs">{t('players.form.lastName')}</span>
          <input
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            className={`${inputClass} mt-0`}
          />
        </label>
        <label className="block">
          <span className="text-sable-700 text-xs">{t('players.form.firstName')}</span>
          <input
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            className={`${inputClass} mt-0`}
          />
        </label>
        <label className="block">
          <span className="text-sable-700 text-xs">{t('players.form.birthDate')}</span>
          <input
            type="date"
            value={birthDate}
            onChange={(e) => setBirthDate(e.target.value)}
            className={`${inputClass} mt-0`}
          />
        </label>
        <label className="block">
          <span className="text-sable-700 text-xs">{t('players.form.gender')}</span>
          <select
            value={gender}
            onChange={(e) => setGender(e.target.value as Gender | '')}
            className={`${inputClass} mt-0`}
          >
            <option value="" disabled>
              {t('registrationImport.values.choose')}
            </option>
            {GENDERS.map((g) => (
              <option key={g} value={g}>
                {t(`players.genders.${g}`)}
              </option>
            ))}
          </select>
        </label>
        {problem.missing.includes('CLUB') && (
          <label className="block sm:col-span-2">
            <span className="text-sable-700 text-xs">{t('players.form.club')}</span>
            <input
              value={clubName}
              onChange={(e) => setClubName(e.target.value)}
              className={`${inputClass} mt-0`}
            />
          </label>
        )}
      </div>
      <div className="flex justify-end">
        <Button
          type="button"
          disabled={!valid}
          onClick={() =>
            onValidate({
              lastName: lastName.trim(),
              firstName: firstName.trim(),
              birthDate,
              gender: gender || undefined,
              clubName: clubName.trim(),
            })
          }
        >
          {t('registrationImport.incomplete.validate')}
        </Button>
      </div>
    </div>
  );
}

export function IncompleteRows({
  problems,
  onFix,
}: {
  problems: RowProblem[];
  onFix: (rowNumber: number, fix: RowFix) => void;
}) {
  const { t } = useTranslation();
  const open = problems.filter((p) => !p.skipped);
  const skipped = problems.filter((p) => p.skipped);
  if (problems.length === 0) return null;

  return (
    <section className="border-sunset-300 space-y-4 rounded-lg border bg-white p-4">
      {open.length > 0 && (
        <div>
          <h2 className="font-display text-base font-semibold">
            {t('registrationImport.incomplete.title', { count: open.length })}
          </h2>
          <p className="text-sable-600 text-sm">{t('registrationImport.incomplete.hint')}</p>
        </div>
      )}

      {open.map((problem) => (
        <article
          key={problem.rowNumber}
          className="border-sable-200 space-y-3 rounded-md border p-4"
        >
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-sm font-medium">
                {t('registrationImport.preview.line', { n: problem.rowNumber })} —{' '}
                {problem.fullName || '—'}
                {problem.draft.clubName && (
                  <span className="text-sable-600 font-normal"> · {problem.draft.clubName}</span>
                )}
              </p>
              <p className="text-sm text-red-700">
                {problem.missing
                  .map((field) => t(`registrationImport.incomplete.missing.${field}`))
                  .join(' · ')}
              </p>
              <p className="text-sable-600 text-xs">
                {t('registrationImport.incomplete.entries', { count: problem.entryCount })}
              </p>
            </div>
            <button
              type="button"
              onClick={() => onFix(problem.rowNumber, { skip: true })}
              className={secondaryButtonClass}
            >
              {t('registrationImport.incomplete.skip')}
            </button>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <PlayerSearch
              initialQuery={longestWord(problem.fullName)}
              onChoose={(player) =>
                onFix(problem.rowNumber, {
                  playerId: player.id,
                  lastName: player.lastName,
                  firstName: player.firstName,
                  birthDate: player.birthDate,
                  gender: player.gender,
                  clubName: problem.draft.clubName || player.club.name,
                })
              }
            />
            <CompleteForm problem={problem} onValidate={(fix) => onFix(problem.rowNumber, fix)} />
          </div>
        </article>
      ))}

      {skipped.length > 0 && (
        <ul className="text-sable-600 space-y-1 text-sm">
          {skipped.map((problem) => (
            <li key={problem.rowNumber} className="flex flex-wrap items-center gap-2">
              {t('registrationImport.incomplete.skipped', {
                n: problem.rowNumber,
                name: problem.fullName || '—',
              })}
              <button
                type="button"
                onClick={() => onFix(problem.rowNumber, {})}
                className="text-lagon-700 hover:underline"
              >
                {t('registrationImport.incomplete.restore')}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
