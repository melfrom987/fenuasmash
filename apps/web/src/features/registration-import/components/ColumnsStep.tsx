import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { inputClass, secondaryButtonClass } from '@/components/ui/styles';
import { cellText } from '../mapping';
import {
  FIELDS,
  REQUIRED_FIELDS,
  type ColumnMapping,
  type DateOrder,
  type NameOrder,
  type Table,
} from '../types';

const sectionClass = 'space-y-4 rounded-lg bg-white p-6 shadow-sm';
const NONE = '';

export function ColumnsStep({
  table,
  mapping,
  onChange,
  onBack,
  onNext,
}: {
  table: Table;
  mapping: ColumnMapping;
  onChange: (mapping: ColumnMapping) => void;
  onBack: () => void;
  onNext: () => void;
}) {
  const { t } = useTranslation();

  const sample = (column: number | null) =>
    column === null
      ? ''
      : (table.rows.map((row) => cellText(row[column])).find((value) => value) ?? '');

  const columnOptions = (
    <>
      <option value={NONE}>{t('registrationImport.columns.none')}</option>
      {table.headers.map((header, index) => (
        <option key={index} value={index}>
          {header || t('registrationImport.columns.untitled', { n: index + 1 })}
        </option>
      ))}
    </>
  );
  const toColumn = (value: string) => (value === NONE ? null : Number(value));

  const missing = REQUIRED_FIELDS.filter((field) => mapping.fields[field] === null);
  const canContinue = missing.length === 0 && mapping.events.length > 0;

  return (
    <div className="space-y-6">
      <p className="text-sable-600 text-sm">
        {t('registrationImport.columns.intro', {
          file: table.fileName,
          count: table.rows.length,
        })}
      </p>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={sectionClass}>
          <h2 className="font-display text-base font-semibold">
            {t('registrationImport.columns.playerTitle')}
          </h2>
          {FIELDS.map((field) => (
            <label key={field} className="block">
              <span className="text-sable-800 text-sm font-medium">
                {t(`registrationImport.fields.${field}`)}
                {REQUIRED_FIELDS.includes(field) && ' *'}
              </span>
              <select
                value={mapping.fields[field] ?? NONE}
                onChange={(e) =>
                  onChange({
                    ...mapping,
                    fields: { ...mapping.fields, [field]: toColumn(e.target.value) },
                  })
                }
                className={inputClass}
              >
                {columnOptions}
              </select>
              {mapping.fields[field] !== null && (
                <span className="text-sable-600 mt-1 block truncate text-xs">
                  {t('registrationImport.columns.example', {
                    value: sample(mapping.fields[field]),
                  })}
                </span>
              )}
            </label>
          ))}

          <fieldset>
            <legend className="text-sable-800 text-sm font-medium">
              {t('registrationImport.columns.nameOrder')}
            </legend>
            <div className="mt-2 flex flex-wrap gap-4">
              {(['LAST_FIRST', 'FIRST_LAST'] as NameOrder[]).map((order) => (
                <label key={order} className="text-sable-800 flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    checked={mapping.nameOrder === order}
                    onChange={() => onChange({ ...mapping, nameOrder: order })}
                    className="accent-lagon-600 size-4"
                  />
                  {t(`registrationImport.columns.nameOrders.${order}`)}
                </label>
              ))}
            </div>
            <p className="text-sable-600 mt-1 text-xs">
              {t('registrationImport.columns.nameOrderHint')}
            </p>
          </fieldset>

          <fieldset>
            <legend className="text-sable-800 text-sm font-medium">
              {t('registrationImport.columns.dateOrder')}
            </legend>
            <div className="mt-2 flex flex-wrap gap-4">
              {(['DMY', 'MDY'] as DateOrder[]).map((order) => (
                <label key={order} className="text-sable-800 flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    checked={mapping.dateOrder === order}
                    onChange={() => onChange({ ...mapping, dateOrder: order })}
                    className="accent-lagon-600 size-4"
                  />
                  {t(`registrationImport.columns.dateOrders.${order}`)}
                </label>
              ))}
            </div>
          </fieldset>
        </section>

        <section className={sectionClass}>
          <div>
            <h2 className="font-display text-base font-semibold">
              {t('registrationImport.columns.eventsTitle')}
            </h2>
            <p className="text-sable-600 text-sm">{t('registrationImport.columns.eventsHint')}</p>
          </div>

          {mapping.events.map((eventColumn, index) => (
            <div key={index} className="border-sable-200 space-y-2 rounded-md border p-3">
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="block">
                  <span className="text-sable-800 text-sm font-medium">
                    {t('registrationImport.columns.eventColumn')}
                  </span>
                  <select
                    value={eventColumn.column}
                    onChange={(e) => {
                      const events = [...mapping.events];
                      events[index] = { ...eventColumn, column: Number(e.target.value) };
                      onChange({ ...mapping, events });
                    }}
                    className={inputClass}
                  >
                    {table.headers.map((header, i) => (
                      <option key={i} value={i}>
                        {header}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="block">
                  <span className="text-sable-800 text-sm font-medium">
                    {t('registrationImport.columns.partnerColumn')}
                  </span>
                  <select
                    value={eventColumn.partnerColumn ?? NONE}
                    onChange={(e) => {
                      const events = [...mapping.events];
                      events[index] = { ...eventColumn, partnerColumn: toColumn(e.target.value) };
                      onChange({ ...mapping, events });
                    }}
                    className={inputClass}
                  >
                    {columnOptions}
                  </select>
                </label>
              </div>
              <button
                type="button"
                onClick={() =>
                  onChange({ ...mapping, events: mapping.events.filter((_, i) => i !== index) })
                }
                className="text-sm text-red-700 hover:underline"
              >
                {t('registrationImport.columns.removeEvent')}
              </button>
            </div>
          ))}

          <button
            type="button"
            onClick={() =>
              onChange({
                ...mapping,
                events: [...mapping.events, { column: 0, partnerColumn: null }],
              })
            }
            className={secondaryButtonClass}
          >
            {t('registrationImport.columns.addEvent')}
          </button>
        </section>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <button type="button" onClick={onBack} className={secondaryButtonClass}>
          {t('registrationImport.back')}
        </button>
        <div className="flex items-center gap-3">
          {!canContinue && (
            <p className="text-sm text-red-700">{t('registrationImport.columns.incomplete')}</p>
          )}
          <Button type="button" disabled={!canContinue} onClick={onNext}>
            {t('registrationImport.next')}
          </Button>
        </div>
      </div>
    </div>
  );
}
