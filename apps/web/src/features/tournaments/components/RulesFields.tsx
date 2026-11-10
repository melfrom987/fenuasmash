import { useFormContext, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import type { Discipline } from '@/api/models';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { DISCIPLINES } from '@/features/events/constants';
import type { TranslationKey } from '@/i18n';
import { DRAW_FORMATS, type RulesValues } from '../rules';

const smallButton =
  'rounded-md border border-sable-300 bg-white px-2 py-1 text-sm hover:bg-sable-50 disabled:opacity-40';

export function RulesFields() {
  const { t } = useTranslation();
  const {
    register,
    setValue,
    control,
    formState: { errors },
  } = useFormContext<{ rules: RulesValues }>();

  const drawFormat = useWatch({ control, name: 'rules.drawFormat' });
  const twoPointLead = useWatch({ control, name: 'rules.scoring.twoPointLead' });
  const selected = useWatch({ control, name: 'rules.disciplines' });
  const available = DISCIPLINES.filter((d) => !selected.includes(d));
  const ruleErrors = errors.rules;

  const setDisciplines = (next: Discipline[]) =>
    setValue('rules.disciplines', next, { shouldDirty: true, shouldValidate: true });

  const move = (index: number, delta: number) => {
    const next = [...selected];
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item);
    setDisciplines(next);
  };

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

        <div className="grid gap-4 sm:grid-cols-2">
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
        </div>

        <label className="text-sable-800 flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="accent-lagon-600 size-4"
            {...register('rules.scoring.twoPointLead')}
          />
          {t('rules.twoPointLead')}
        </label>
        <p className="text-sable-600 text-xs">{t('rules.twoPointLeadHint')}</p>

        {twoPointLead && (
          <Field
            label={t('rules.pointCap')}
            error={ruleErrors?.scoring?.pointCap?.message}
            hint={t('rules.pointCapHint')}
          >
            <input
              type="number"
              min={1}
              max={50}
              className={`${inputClass} tabular-nums sm:max-w-40`}
              {...register('rules.scoring.pointCap', { valueAsNumber: true })}
            />
          </Field>
        )}
      </fieldset>

      <fieldset className="border-sable-200 space-y-3 rounded-lg border p-4">
        <legend className="text-sable-800 px-1 text-sm font-medium">
          {t('rules.disciplines')}
        </legend>
        <p className="text-sable-600 text-xs">{t('rules.disciplinesHint')}</p>

        {selected.length > 0 && (
          <ol className="divide-sable-200 border-sable-200 divide-y rounded-md border">
            {selected.map((discipline, index) => (
              <li key={discipline} className="flex items-center justify-between gap-3 px-3 py-2">
                <span className="text-sm">
                  <span className="text-sable-600 mr-2 tabular-nums">{index + 1}.</span>
                  {t(`events.disciplines.${discipline}`)}
                </span>
                <span className="flex gap-1">
                  <button
                    type="button"
                    aria-label={t('rules.moveUp')}
                    disabled={index === 0}
                    onClick={() => move(index, -1)}
                    className={smallButton}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    aria-label={t('rules.moveDown')}
                    disabled={index === selected.length - 1}
                    onClick={() => move(index, 1)}
                    className={smallButton}
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    aria-label={t('rules.remove')}
                    onClick={() => setDisciplines(selected.filter((d) => d !== discipline))}
                    className={smallButton}
                  >
                    ×
                  </button>
                </span>
              </li>
            ))}
          </ol>
        )}

        {available.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {available.map((discipline) => (
              <button
                key={discipline}
                type="button"
                onClick={() => setDisciplines([...selected, discipline])}
                className={smallButton}
              >
                + {t(`events.disciplines.${discipline}`)}
              </button>
            ))}
          </div>
        )}

        {ruleErrors?.disciplines?.message && (
          <p className="text-sm text-red-700">
            {t(ruleErrors.disciplines.message as TranslationKey)}
          </p>
        )}
      </fieldset>
    </div>
  );
}
