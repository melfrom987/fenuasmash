import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { CategoryType, Discipline } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { useCreateEvent } from '@/features/events/api';
import { CATEGORY_TYPES, DISCIPLINES } from '@/features/events/constants';
import { categoryBirthYears } from '@/features/players/age-category';
import { normalizeText } from '@/lib/text';
import { useErrorMessage } from '@/lib/use-error-message';
import { guessCategory, guessDiscipline } from '../event-guess';
import { distinctValues, isYesValue } from '../mapping';
import type { ColumnMapping, Table, ValueMapping } from '../types';

export type EventAssignment = { column: number; value: string; eventId: string };

type Source = { column: number; value: string };

type Draft = {
  key: string;
  sources: Source[];
  include: boolean;
  name: string;
  nameEdited: boolean;
  discipline: Discipline;
  categoryType: CategoryType;
  categoryLabel: string;
  created: boolean;
  error: string | null;
};

const cellClass = 'px-2 py-2 align-top';

export function EventDraftsPanel({
  tournamentId,
  table,
  mapping,
  values,
  offeredDisciplines,
  seasonYear,
  onCreated,
  onClose,
}: {
  tournamentId: string;
  table: Table;
  mapping: ColumnMapping;
  values: ValueMapping;
  offeredDisciplines: readonly Discipline[];
  seasonYear: number;
  onCreated: (assignments: EventAssignment[]) => void;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const createEvent = useCreateEvent(tournamentId);
  const [creating, setCreating] = useState(false);

  const autoName = (discipline: Discipline, categoryLabel: string) =>
    [t(`events.disciplines.${discipline}`), categoryLabel.trim()].filter(Boolean).join(' ');

  const [drafts, setDrafts] = useState<Draft[]>(() => {
    const byKey = new Map<string, Draft>();
    for (const { column } of mapping.events) {
      const header = table.headers[column] ?? '';
      for (const { value } of distinctValues(table, column)) {
        if (values.events[column]?.[value]) continue;
        const text = isYesValue(value) ? header : value;
        const discipline = guessDiscipline(text, header);
        const { categoryType, categoryLabel } = guessCategory(text);
        const key = `${discipline}|${categoryType}|${normalizeText(categoryLabel)}`;
        const existing = byKey.get(key);
        if (existing) {
          existing.sources.push({ column, value });
          continue;
        }
        byKey.set(key, {
          key,
          sources: [{ column, value }],
          include: true,
          name: autoName(discipline, categoryLabel),
          nameEdited: false,
          discipline,
          categoryType,
          categoryLabel,
          created: false,
          error: null,
        });
      }
    }
    return [...byKey.values()];
  });

  const update = (key: string, change: Partial<Draft>) =>
    setDrafts((list) =>
      list.map((draft) => {
        if (draft.key !== key) return draft;
        const next = { ...draft, ...change };
        return next.nameEdited
          ? next
          : {
              ...next,
              name: autoName(
                next.discipline,
                next.categoryType === 'OPEN' ? '' : next.categoryLabel,
              ),
            };
      }),
    );

  const isValid = (draft: Draft) =>
    draft.name.trim() !== '' &&
    (draft.categoryType === 'OPEN' || draft.categoryLabel.trim() !== '');
  const selected = drafts.filter((d) => d.include && !d.created);
  const canCreate = selected.length > 0 && selected.every(isValid) && !creating;

  const create = async () => {
    setCreating(true);
    const assignments: EventAssignment[] = [];
    const next: Draft[] = [];
    for (const draft of drafts) {
      if (!draft.include || draft.created) {
        next.push(draft);
        continue;
      }
      try {
        const bounds =
          draft.categoryType === 'AGE' ? categoryBirthYears(draft.categoryLabel, seasonYear) : null;
        const event = await createEvent.mutateAsync({
          name: draft.name.trim(),
          discipline: draft.discipline,
          categoryType: draft.categoryType,
          categoryLabel: draft.categoryType === 'OPEN' ? null : draft.categoryLabel.trim(),
          plannedDate: null,
          maxEntries: null,
          minBirthYear: bounds?.minBirthYear ?? null,
          maxBirthYear: bounds?.maxBirthYear ?? null,
        });
        for (const source of draft.sources) assignments.push({ ...source, eventId: event.id });
        next.push({ ...draft, created: true, error: null });
      } catch (error) {
        next.push({
          ...draft,
          error: errorMessage(error instanceof Error ? error : new Error(String(error))),
        });
      }
    }
    setDrafts(next);
    setCreating(false);
    onCreated(assignments);
    if (next.every((d) => d.created || !d.include)) onClose();
  };

  return (
    <div className="border-lagon-200 bg-lagon-50 space-y-4 rounded-lg border p-4">
      <div>
        <h3 className="font-display text-base font-semibold">
          {t('registrationImport.drafts.title')}
        </h3>
        <p className="text-sable-700 text-sm">{t('registrationImport.drafts.hint')}</p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[720px] text-sm">
          <thead>
            <tr className="text-sable-600 text-left text-xs tracking-wide uppercase">
              <th className="w-8 px-2 py-1">
                <span className="sr-only">{t('registrationImport.drafts.include')}</span>
              </th>
              <th className="px-2 py-1 font-medium">{t('registrationImport.drafts.answers')}</th>
              <th className="px-2 py-1 font-medium">{t('registrationImport.drafts.name')}</th>
              <th className="px-2 py-1 font-medium">{t('registrationImport.drafts.discipline')}</th>
              <th className="px-2 py-1 font-medium">{t('registrationImport.drafts.category')}</th>
            </tr>
          </thead>
          <tbody className="divide-lagon-100 divide-y">
            {drafts.map((draft) => (
              <tr key={draft.key} className={draft.created ? 'opacity-60' : ''}>
                <td className={cellClass}>
                  <input
                    type="checkbox"
                    checked={draft.include && !draft.created}
                    disabled={draft.created || creating}
                    onChange={(e) => update(draft.key, { include: e.target.checked })}
                    className="accent-lagon-600 mt-2 size-4"
                    aria-label={t('registrationImport.drafts.include')}
                  />
                </td>
                <td className={`${cellClass} text-sable-700 max-w-48`}>
                  {draft.sources.map((s) => s.value).join(', ')}
                  {draft.created && (
                    <p className="text-lagon-700 text-xs font-medium">
                      {t('registrationImport.drafts.created')}
                    </p>
                  )}
                  {draft.error && <p className="text-xs text-red-700">{draft.error}</p>}
                </td>
                <td className={cellClass}>
                  <input
                    value={draft.name}
                    disabled={draft.created || creating}
                    onChange={(e) => update(draft.key, { name: e.target.value, nameEdited: true })}
                    className={`${inputClass} mt-0`}
                  />
                </td>
                <td className={cellClass}>
                  <select
                    value={draft.discipline}
                    disabled={draft.created || creating}
                    onChange={(e) =>
                      update(draft.key, { discipline: e.target.value as Discipline })
                    }
                    className={`${inputClass} mt-0`}
                  >
                    {DISCIPLINES.map((discipline) => (
                      <option key={discipline} value={discipline}>
                        {t(`events.disciplines.${discipline}`)}
                      </option>
                    ))}
                  </select>
                  {!offeredDisciplines.includes(draft.discipline) && (
                    <p className="text-sunset-800 mt-1 text-xs">
                      {t('registrationImport.drafts.notOffered')}
                    </p>
                  )}
                </td>
                <td className={cellClass}>
                  <div className="flex gap-2">
                    <select
                      value={draft.categoryType}
                      disabled={draft.created || creating}
                      onChange={(e) =>
                        update(draft.key, { categoryType: e.target.value as CategoryType })
                      }
                      className={`${inputClass} mt-0 w-auto`}
                    >
                      {CATEGORY_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {t(`events.categoryTypes.${type}`)}
                        </option>
                      ))}
                    </select>
                    {draft.categoryType !== 'OPEN' && (
                      <input
                        value={draft.categoryLabel}
                        disabled={draft.created || creating}
                        onChange={(e) => update(draft.key, { categoryLabel: e.target.value })}
                        className={`${inputClass} mt-0`}
                      />
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          disabled={creating}
          className={secondaryButtonClass}
        >
          {t('common.cancel')}
        </button>
        <Button type="button" disabled={!canCreate} onClick={() => void create()}>
          {creating
            ? t('registrationImport.drafts.creating')
            : t('registrationImport.drafts.create', { count: selected.length })}
        </Button>
      </div>
    </div>
  );
}
