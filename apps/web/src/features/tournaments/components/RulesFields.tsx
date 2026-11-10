import { useFieldArray, useFormContext, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { DISCIPLINES } from '@/features/events/constants';
import type { TranslationKey } from '@/i18n';
import { DRAW_FORMATS, type RulesValues } from '../rules';

const smallButton =
  'rounded-md border border-sable-300 bg-white px-3 py-1.5 text-sm hover:bg-sable-50 disabled:opacity-40';
const selectClass = 'rounded-md border border-sable-300 bg-white px-3 py-2 text-sm';

export function RulesFields() {
  const { t } = useTranslation();
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<{ rules: RulesValues }>();

  const drawFormat = useWatch({ control, name: 'rules.drawFormat' });
  const twoPointLead = useWatch({ control, name: 'rules.scoring.twoPointLead' });
  const watchedDisciplines = useWatch({ control, name: 'rules.disciplines' });
  const offered = DISCIPLINES.filter(
    (d) => Array.isArray(watchedDisciplines) && watchedDisciplines.includes(d),
  );
  const combinations = useFieldArray({ control, name: 'rules.forbiddenCombinations' });
  const ruleErrors = errors.rules;

  return (
    <div className="space-y-5">
      <Field
        label={t('rules.drawFormat')}
        error={ruleErrors?.drawFormat?.message}
        hint={t(`rules.drawFormatHints.${drawFormat}`)}
      >
        <select className={inputClass} {...register('rules.drawFormat')}>
          {DRAW_FORMATS.map((format) => (
            <option key={format} value={format}>
              {t(`rules.drawFormats.${format}`)}
            </option>
          ))}
        </select>
      </Field>

      <fieldset className="border-sable-200 space-y-3 rounded-lg border p-4">
        <legend className="text-sable-800 px-1 text-sm font-medium">{t('rules.scoring')}</legend>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label={t('rules.gamesToWin')} error={ruleErrors?.scoring?.gamesToWin?.message}>
            <input
              type="number"
              min={1}
              max={3}
              className={`${inputClass} tabular-nums`}
              {...register('rules.scoring.gamesToWin', { valueAsNumber: true })}
            />
          </Field>
          <Field label={t('rules.pointsToWin')} error={ruleErrors?.scoring?.pointsToWin?.message}>
            <input
              type="number"
              min={1}
              max={50}
              className={`${inputClass} tabular-nums`}
              {...register('rules.scoring.pointsToWin', { valueAsNumber: true })}
            />
          </Field>
          {twoPointLead && (
            <Field label={t('rules.pointCap')} error={ruleErrors?.scoring?.pointCap?.message}>
              <input
                type="number"
                min={1}
                max={50}
                className={`${inputClass} tabular-nums`}
                {...register('rules.scoring.pointCap', { valueAsNumber: true })}
              />
            </Field>
          )}
        </div>

        <label className="text-sable-800 flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="accent-lagon-600 size-4"
            {...register('rules.scoring.twoPointLead')}
          />
          {t('rules.twoPointLead')}
        </label>
        <p className="text-sable-600 text-xs">
          {twoPointLead ? t('rules.pointCapHint') : t('rules.twoPointLeadHint')}
        </p>
      </fieldset>

      <fieldset className="border-sable-200 space-y-3 rounded-lg border p-4">
        <legend className="text-sable-800 px-1 text-sm font-medium">
          {t('rules.disciplines')}
        </legend>
        <p className="text-sable-600 text-xs">{t('rules.disciplinesHint')}</p>
        <div className="grid gap-2 sm:grid-cols-3">
          {DISCIPLINES.map((discipline) => (
            <label key={discipline} className="text-sable-800 flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                value={discipline}
                className="accent-lagon-600 size-4"
                {...register('rules.disciplines')}
              />
              {t(`events.disciplines.${discipline}`)}
            </label>
          ))}
        </div>
        {ruleErrors?.disciplines?.message && (
          <p className="text-sm text-red-700">
            {t(ruleErrors.disciplines.message as TranslationKey)}
          </p>
        )}
      </fieldset>

      <fieldset className="border-sable-200 space-y-3 rounded-lg border p-4">
        <legend className="text-sable-800 px-1 text-sm font-medium">
          {t('rules.forbiddenCombinations')}
        </legend>
        <p className="text-sable-600 text-xs">{t('rules.forbiddenCombinationsHint')}</p>

        {combinations.fields.map((field, index) => {
          const error = ruleErrors?.forbiddenCombinations?.[index]?.second?.message;
          return (
            <div key={field.id} className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <select
                  aria-label={t('rules.combinationFirst')}
                  className={selectClass}
                  {...register(`rules.forbiddenCombinations.${index}.first`)}
                >
                  {DISCIPLINES.map((d) => (
                    <option key={d} value={d}>
                      {t(`events.disciplines.${d}`)}
                    </option>
                  ))}
                </select>
                <span className="text-sable-600 text-sm">{t('rules.combinationWith')}</span>
                <select
                  aria-label={t('rules.combinationSecond')}
                  className={selectClass}
                  {...register(`rules.forbiddenCombinations.${index}.second`)}
                >
                  {DISCIPLINES.map((d) => (
                    <option key={d} value={d}>
                      {t(`events.disciplines.${d}`)}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  aria-label={t('rules.remove')}
                  onClick={() => combinations.remove(index)}
                  className={smallButton}
                >
                  ×
                </button>
              </div>
              {error && <p className="text-sm text-red-700">{t(error as TranslationKey)}</p>}
            </div>
          );
        })}

        <button
          type="button"
          disabled={offered.length < 2}
          onClick={() => combinations.append({ first: offered[0], second: offered[1] })}
          className={smallButton}
        >
          + {t('rules.addCombination')}
        </button>
      </fieldset>
    </div>
  );
}
