import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Club, Discipline, Event, Gender } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { GENDERS } from '@/features/players/constants';
import { distinctValues, newClubNames } from '../mapping';
import { IGNORE, type ColumnMapping, type Table, type ValueMapping } from '../types';
import { EventDraftsPanel, type EventAssignment } from './EventDraftsPanel';

const sectionClass = 'space-y-4 rounded-lg bg-white p-6 shadow-sm';

export function ValuesStep({
  tournamentId,
  table,
  mapping,
  values,
  events,
  clubs,
  offeredDisciplines,
  seasonYear,
  pending,
  onChange,
  onBack,
  onNext,
}: {
  tournamentId: string;
  table: Table;
  mapping: ColumnMapping;
  values: ValueMapping;
  events: Event[];
  clubs: Club[];
  offeredDisciplines: readonly Discipline[];
  seasonYear: number;
  pending: boolean;
  onChange: (values: ValueMapping) => void;
  onBack: () => void;
  onNext: () => void;
}) {
  const { t } = useTranslation();
  const [draftsOpen, setDraftsOpen] = useState(false);

  const eventColumns = mapping.events.map(({ column }) => ({
    column,
    values: distinctValues(table, column),
  }));
  const genderValues =
    mapping.fields.gender === null ? [] : distinctValues(table, mapping.fields.gender);
  const newClubs = newClubNames(table, mapping, clubs);

  const answers = eventColumns.flatMap(({ column, values: list }) =>
    list.map(({ value }) => values.events[column]?.[value]),
  );
  const unmatched = answers.filter((target) => !target).length;

  const complete =
    unmatched === 0 &&
    genderValues.every(({ value }) => values.genders[value]) &&
    newClubs.every(({ key }) => values.clubCities[key]?.trim());

  const setEventValue = (column: number, value: string, target: string) =>
    onChange({
      ...values,
      events: { ...values.events, [column]: { ...values.events[column], [value]: target } },
    });

  const applyAssignments = (assignments: EventAssignment[]) => {
    if (assignments.length === 0) return;
    const nextEvents = { ...values.events };
    for (const { column, value, eventId } of assignments) {
      nextEvents[column] = { ...nextEvents[column], [value]: eventId };
    }
    onChange({ ...values, events: nextEvents });
  };

  return (
    <div className="space-y-6">
      <section className={sectionClass}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="font-display text-base font-semibold">
              {t('registrationImport.values.eventsTitle')}
            </h2>
            <p className="text-sable-600 text-sm">{t('registrationImport.values.eventsHint')}</p>
          </div>
          {unmatched > 0 && !draftsOpen && (
            <button
              type="button"
              onClick={() => setDraftsOpen(true)}
              className={secondaryButtonClass}
            >
              {t('registrationImport.values.createMissing')}
            </button>
          )}
        </div>

        {draftsOpen && (
          <EventDraftsPanel
            tournamentId={tournamentId}
            table={table}
            mapping={mapping}
            values={values}
            offeredDisciplines={offeredDisciplines}
            seasonYear={seasonYear}
            onCreated={applyAssignments}
            onClose={() => setDraftsOpen(false)}
          />
        )}

        {eventColumns.map(({ column, values: list }) => (
          <div key={column} className="space-y-2">
            <h3 className="text-sable-800 text-sm font-semibold">{table.headers[column]}</h3>
            <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {list.map(({ value, count }) => {
                const target = values.events[column]?.[value] ?? '';
                return (
                  <li
                    key={value}
                    className={`rounded-md border p-3 ${
                      target ? 'border-sable-200' : 'border-sunset-400 bg-sunset-50'
                    }`}
                  >
                    <div className="flex items-baseline justify-between gap-2">
                      <span className="truncate text-sm font-medium" title={value}>
                        {value}
                      </span>
                      <span className="text-sable-600 shrink-0 text-xs tabular-nums">
                        {t('registrationImport.values.rows', { count })}
                      </span>
                    </div>
                    <select
                      value={target}
                      onChange={(e) => setEventValue(column, value, e.target.value)}
                      className={`${inputClass} mt-2`}
                    >
                      <option value="" disabled>
                        {t('registrationImport.values.choose')}
                      </option>
                      {events.map((event) => (
                        <option key={event.id} value={event.id}>
                          {event.name}
                        </option>
                      ))}
                      <option value={IGNORE}>{t('registrationImport.values.ignore')}</option>
                    </select>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </section>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={sectionClass}>
          <h2 className="font-display text-base font-semibold">
            {t('registrationImport.values.gendersTitle')}
          </h2>
          <ul className="grid gap-3 sm:grid-cols-2">
            {genderValues.map(({ value, count }) => (
              <li key={value} className="border-sable-200 rounded-md border p-3">
                <div className="flex items-baseline justify-between gap-2">
                  <span className="truncate text-sm font-medium">{value}</span>
                  <span className="text-sable-600 shrink-0 text-xs tabular-nums">
                    {t('registrationImport.values.rows', { count })}
                  </span>
                </div>
                <select
                  value={values.genders[value] ?? ''}
                  onChange={(e) =>
                    onChange({
                      ...values,
                      genders: { ...values.genders, [value]: e.target.value as Gender },
                    })
                  }
                  className={`${inputClass} mt-2`}
                >
                  <option value="" disabled>
                    {t('registrationImport.values.choose')}
                  </option>
                  {GENDERS.map((gender) => (
                    <option key={gender} value={gender}>
                      {t(`players.genders.${gender}`)}
                    </option>
                  ))}
                </select>
              </li>
            ))}
          </ul>
        </section>

        {newClubs.length > 0 && (
          <section className={sectionClass}>
            <div>
              <h2 className="font-display text-base font-semibold">
                {t('registrationImport.values.clubsTitle')}
              </h2>
              <p className="text-sable-600 text-sm">{t('registrationImport.values.clubsHint')}</p>
            </div>
            <ul className="grid gap-3 sm:grid-cols-2">
              {newClubs.map(({ key, name }) => (
                <li key={key}>
                  <label className="block">
                    <span className="text-sable-800 text-sm font-medium">{name}</span>
                    <input
                      value={values.clubCities[key] ?? ''}
                      placeholder={t('registrationImport.values.cityPlaceholder')}
                      onChange={(e) =>
                        onChange({
                          ...values,
                          clubCities: { ...values.clubCities, [key]: e.target.value },
                        })
                      }
                      className={inputClass}
                    />
                  </label>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>

      <div className="border-sable-200 bg-sable-50/95 sticky bottom-0 -mx-6 flex flex-wrap items-center justify-between gap-3 border-t px-6 py-3 backdrop-blur">
        <button type="button" onClick={onBack} className={secondaryButtonClass}>
          {t('registrationImport.back')}
        </button>
        <div className="flex flex-wrap items-center gap-3">
          <p className={`text-sm ${complete ? 'text-sable-700' : 'text-red-700'}`}>
            {complete
              ? t('registrationImport.values.ready')
              : t('registrationImport.values.progress', {
                  done: answers.length - unmatched,
                  total: answers.length,
                })}
          </p>
          <Button type="button" disabled={!complete || pending} onClick={onNext}>
            {pending
              ? t('registrationImport.values.checking')
              : t('registrationImport.values.preview')}
          </Button>
        </div>
      </div>
    </div>
  );
}
