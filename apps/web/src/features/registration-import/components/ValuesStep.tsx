import { useTranslation } from 'react-i18next';
import type { Club, Event, Gender } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { GENDERS } from '@/features/players/constants';
import { distinctValues, newClubNames } from '../mapping';
import { IGNORE, type ColumnMapping, type Table, type ValueMapping } from '../types';

const sectionClass = 'space-y-4 rounded-lg bg-white p-6 shadow-sm';
const cellClass = 'py-2 pr-3 text-sm';

export function ValuesStep({
  table,
  mapping,
  values,
  events,
  clubs,
  pending,
  onChange,
  onBack,
  onNext,
}: {
  table: Table;
  mapping: ColumnMapping;
  values: ValueMapping;
  events: Event[];
  clubs: Club[];
  pending: boolean;
  onChange: (values: ValueMapping) => void;
  onBack: () => void;
  onNext: () => void;
}) {
  const { t } = useTranslation();

  const eventColumns = mapping.events.map(({ column }) => ({
    column,
    values: distinctValues(table, column),
  }));
  const genderValues =
    mapping.fields.gender === null ? [] : distinctValues(table, mapping.fields.gender);
  const newClubs = newClubNames(table, mapping, clubs);

  const complete =
    eventColumns.every(({ column, values: list }) =>
      list.every(({ value }) => values.events[column]?.[value]),
    ) &&
    genderValues.every(({ value }) => values.genders[value]) &&
    newClubs.every(({ key }) => values.clubCities[key]?.trim());

  const setEventValue = (column: number, value: string, target: string) =>
    onChange({
      ...values,
      events: { ...values.events, [column]: { ...values.events[column], [value]: target } },
    });

  return (
    <div className="space-y-6">
      <section className={sectionClass}>
        <div>
          <h2 className="font-display text-base font-semibold">
            {t('registrationImport.values.eventsTitle')}
          </h2>
          <p className="text-sable-600 text-sm">{t('registrationImport.values.eventsHint')}</p>
        </div>
        {eventColumns.map(({ column, values: list }) => (
          <div key={column} className="space-y-2">
            <h3 className="text-sable-800 text-sm font-semibold">{table.headers[column]}</h3>
            <table className="w-full">
              <tbody className="divide-sable-200 divide-y">
                {list.map(({ value, count }) => (
                  <tr key={value}>
                    <td className={cellClass}>{value}</td>
                    <td className={`${cellClass} text-sable-600 w-24 tabular-nums`}>
                      {t('registrationImport.values.rows', { count })}
                    </td>
                    <td className="w-1/2 py-2">
                      <select
                        value={values.events[column]?.[value] ?? ''}
                        onChange={(e) => setEventValue(column, value, e.target.value)}
                        className={`${inputClass} mt-0 ${
                          values.events[column]?.[value] ? '' : 'border-sunset-400'
                        }`}
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
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </section>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={sectionClass}>
          <h2 className="font-display text-base font-semibold">
            {t('registrationImport.values.gendersTitle')}
          </h2>
          <table className="w-full">
            <tbody className="divide-sable-200 divide-y">
              {genderValues.map(({ value, count }) => (
                <tr key={value}>
                  <td className={cellClass}>{value}</td>
                  <td className={`${cellClass} text-sable-600 tabular-nums`}>
                    {t('registrationImport.values.rows', { count })}
                  </td>
                  <td className="py-2">
                    <select
                      value={values.genders[value] ?? ''}
                      onChange={(e) =>
                        onChange({
                          ...values,
                          genders: { ...values.genders, [value]: e.target.value as Gender },
                        })
                      }
                      className={`${inputClass} mt-0`}
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
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {newClubs.length > 0 && (
          <section className={sectionClass}>
            <div>
              <h2 className="font-display text-base font-semibold">
                {t('registrationImport.values.clubsTitle')}
              </h2>
              <p className="text-sable-600 text-sm">{t('registrationImport.values.clubsHint')}</p>
            </div>
            {newClubs.map(({ key, name }) => (
              <label key={key} className="block">
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
            ))}
          </section>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <button type="button" onClick={onBack} className={secondaryButtonClass}>
          {t('registrationImport.back')}
        </button>
        <div className="flex items-center gap-3">
          {!complete && (
            <p className="text-sm text-red-700">{t('registrationImport.values.incomplete')}</p>
          )}
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
