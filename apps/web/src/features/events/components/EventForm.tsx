import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { key } from '@/i18n';
import { categoryBirthYears } from '@/features/players/age-category';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import {
  AGE_CATEGORY_SUGGESTIONS,
  CATEGORY_TYPES,
  DISCIPLINES,
  LEVEL_CATEGORY_SUGGESTIONS,
} from '../constants';
import { eventFormSchema, type EventFormValues } from '../schema';

export function EventForm({
  defaultValues,
  seasonYear,
  onSubmit,
}: {
  defaultValues?: EventFormValues;
  seasonYear: number;
  onSubmit: (values: EventFormValues) => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const [formError, setFormError] = useState<Error | null>(null);
  const isCreation = !defaultValues;

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    getValues,
    control,
    formState: { errors, isSubmitting, dirtyFields },
  } = useForm<EventFormValues>({
    resolver: zodResolver(eventFormSchema),
    defaultValues: defaultValues ?? {
      name: t('events.disciplines.MS'),
      discipline: 'MS',
      categoryType: 'OPEN',
      categoryLabel: '',
      plannedDate: '',
      plannedEndDate: '',
      thirdPlaceMatch: false,
      maxEntries: Number.NaN,
      minBirthYear: Number.NaN,
      maxBirthYear: Number.NaN,
    },
  });

  const categoryType = useWatch({ control, name: 'categoryType' });

  const syncName = () => {
    if (!isCreation || dirtyFields.name) return;
    const { discipline, categoryType: type, categoryLabel } = getValues();
    const base = t(`events.disciplines.${discipline}`);
    const label = categoryLabel.trim();
    setValue('name', type !== 'OPEN' && label ? `${base} ${label}` : base);
  };

  const fillBirthYears = (force: boolean) => {
    if (!force && (dirtyFields.minBirthYear || dirtyFields.maxBirthYear)) return;
    const { categoryType: type, categoryLabel } = getValues();
    const bounds = type === 'AGE' ? categoryBirthYears(categoryLabel, seasonYear) : null;
    if (!bounds && !force) return;
    setValue('minBirthYear', bounds?.minBirthYear ?? Number.NaN, { shouldDirty: force });
    setValue('maxBirthYear', bounds?.maxBirthYear ?? Number.NaN, { shouldDirty: force });
  };

  const submit = async (values: EventFormValues) => {
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
        EVENT_NAME_ALREADY_EXISTS: { field: 'name', message: key('events.form.errors.nameTaken') },
      });
      if (!shownOnField) setFormError(error instanceof Error ? error : new Error(String(error)));
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} noValidate className="space-y-5">
      {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label={t('events.form.discipline')} error={errors.discipline?.message}>
          <select className={inputClass} {...register('discipline', { onChange: syncName })}>
            {DISCIPLINES.map((d) => (
              <option key={d} value={d}>
                {t(`events.disciplines.${d}`)}
              </option>
            ))}
          </select>
        </Field>

        <Field label={t('events.form.categoryType')} error={errors.categoryType?.message}>
          <select
            className={inputClass}
            {...register('categoryType', {
              onChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
                if (e.target.value === 'OPEN') setValue('categoryLabel', '');
                if (e.target.value !== 'AGE') {
                  setValue('minBirthYear', Number.NaN);
                  setValue('maxBirthYear', Number.NaN);
                }
                syncName();
              },
            })}
          >
            {CATEGORY_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`events.categoryTypes.${type}`)}
              </option>
            ))}
          </select>
        </Field>
      </div>

      {categoryType !== 'OPEN' && (
        <Field
          label={t('events.form.categoryLabel')}
          error={errors.categoryLabel?.message}
          hint={t('events.form.categoryLabelHint')}
        >
          <input
            className={inputClass}
            list={categoryType === 'AGE' ? 'event-age-categories' : 'event-level-categories'}
            {...register('categoryLabel', {
              onChange: () => {
                syncName();
                fillBirthYears(false);
              },
            })}
          />
        </Field>
      )}

      {categoryType === 'AGE' && (
        <div className="bg-sable-50 space-y-3 rounded-md p-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={t('events.form.minBirthYear')} error={errors.minBirthYear?.message}>
              <input
                type="number"
                className={`${inputClass} tabular-nums`}
                {...register('minBirthYear', { valueAsNumber: true })}
              />
            </Field>
            <Field label={t('events.form.maxBirthYear')} error={errors.maxBirthYear?.message}>
              <input
                type="number"
                className={`${inputClass} tabular-nums`}
                {...register('maxBirthYear', { valueAsNumber: true })}
              />
            </Field>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sable-600 text-xs">
              {t('events.form.birthYearsHint', { season: `${seasonYear - 1}–${seasonYear}` })}
            </p>
            <button
              type="button"
              onClick={() => fillBirthYears(true)}
              className="text-lagon-700 text-sm hover:underline"
            >
              {t('events.form.computeBirthYears')}
            </button>
          </div>
        </div>
      )}
      <datalist id="event-age-categories">
        {AGE_CATEGORY_SUGGESTIONS.map((label) => (
          <option key={label} value={label} />
        ))}
      </datalist>
      <datalist id="event-level-categories">
        {LEVEL_CATEGORY_SUGGESTIONS.map((label) => (
          <option key={label} value={label} />
        ))}
      </datalist>

      <Field
        label={t('events.form.name')}
        error={errors.name?.message}
        hint={isCreation ? t('events.form.nameHint') : undefined}
      >
        <input className={inputClass} {...register('name')} />
      </Field>

      <Field
        label={t('events.form.maxEntries')}
        error={errors.maxEntries?.message}
        hint={t('events.form.maxEntriesHint')}
      >
        <input
          type="number"
          min={2}
          max={256}
          className={`${inputClass} tabular-nums sm:w-40`}
          {...register('maxEntries', { valueAsNumber: true })}
        />
      </Field>

      <fieldset className="space-y-3">
        <legend className="text-sable-800 text-sm font-medium">{t('events.form.slot')}</legend>
        <p className="text-sable-600 text-xs">{t('events.form.slotHint')}</p>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t('events.form.plannedDate')} error={errors.plannedDate?.message}>
            <input type="datetime-local" className={inputClass} {...register('plannedDate')} />
          </Field>
          <Field label={t('events.form.plannedEndDate')} error={errors.plannedEndDate?.message}>
            <input type="datetime-local" className={inputClass} {...register('plannedEndDate')} />
          </Field>
        </div>
      </fieldset>

      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" className="mt-1" {...register('thirdPlaceMatch')} />
        <span>
          <span className="font-medium">{t('events.form.thirdPlaceMatch')}</span>
          <span className="text-sable-600 block">{t('events.form.thirdPlaceMatchHint')}</span>
        </span>
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting
          ? t('events.form.saving')
          : isCreation
            ? t('events.form.create')
            : t('events.form.save')}
      </Button>
    </form>
  );
}
