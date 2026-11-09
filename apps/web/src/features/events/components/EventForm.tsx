import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import type { EventType } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { key } from '@/i18n';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import { DEFAULT_SCORING, EVENT_FORMATS, EVENT_TYPES } from '../constants';
import { eventFormSchema, type EventFormValues } from '../schema';

export function EventForm({
  defaultValues,
  onSubmit,
}: {
  /** Absent = création ; présent = modification */
  defaultValues?: EventFormValues;
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
    getFieldState,
    control,
    formState: { errors, isSubmitting },
  } = useForm<EventFormValues>({
    resolver: zodResolver(eventFormSchema),
    defaultValues: defaultValues ?? {
      name: t('events.types.MS'),
      type: 'MS',
      format: 'SINGLE_ELIMINATION',
      poolSize: 4,
      qualifiersPerPool: 2,
      scoring: DEFAULT_SCORING,
    },
  });

  const format = useWatch({ control, name: 'format' });

  // En création, le nom suit le type tant que l'organisateur ne l'a pas modifié
  const typeField = register('type', {
    onChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
      if (isCreation && !getFieldState('name').isDirty) {
        setValue('name', t(`events.types.${e.target.value as EventType}`));
      }
    },
  });

  const submit = async (values: EventFormValues) => {
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
        EVENT_NAME_ALREADY_EXISTS: { field: 'name', message: key('events.form.errors.nameTaken') },
        INVALID_FORMAT_SETTINGS: {
          field: 'qualifiersPerPool',
          message: key('events.form.errors.qualifiersRange'),
        },
        INVALID_SCORING_RULES: {
          field: 'scoring.pointCap',
          message: key('events.form.errors.pointCapBelowPoints'),
        },
      });
      if (!shownOnField) setFormError(error instanceof Error ? error : new Error(String(error)));
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} noValidate className="space-y-5">
      {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label={t('events.form.type')} error={errors.type?.message}>
          <select className={inputClass} {...typeField}>
            {EVENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`events.types.${type}`)}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label={t('events.form.name')}
          error={errors.name?.message}
          hint={isCreation ? t('events.form.nameHint') : undefined}
        >
          <input className={inputClass} {...register('name')} />
        </Field>
      </div>

      <Field
        label={t('events.form.format')}
        error={errors.format?.message}
        hint={t(`events.formatHints.${format}`)}
      >
        <select className={inputClass} {...register('format')}>
          {EVENT_FORMATS.map((f) => (
            <option key={f} value={f}>
              {t(`events.formats.${f}`)}
            </option>
          ))}
        </select>
      </Field>

      {format === 'POOLS_THEN_ELIMINATION' && (
        <div className="bg-lagon-50 grid gap-4 rounded-lg p-4 sm:grid-cols-2">
          <Field label={t('events.form.poolSize')} error={errors.poolSize?.message}>
            <input
              type="number"
              min={3}
              max={8}
              className={inputClass}
              {...register('poolSize', { valueAsNumber: true })}
            />
          </Field>
          <Field
            label={t('events.form.qualifiersPerPool')}
            error={errors.qualifiersPerPool?.message}
          >
            <input
              type="number"
              min={1}
              max={7}
              className={inputClass}
              {...register('qualifiersPerPool', { valueAsNumber: true })}
            />
          </Field>
        </div>
      )}

      <fieldset className="border-sable-200 space-y-3 rounded-lg border p-4">
        <legend className="text-sable-800 px-1 text-sm font-medium">
          {t('events.form.scoring')}
        </legend>
        <p className="text-sable-600 text-xs">{t('events.form.scoringHint')}</p>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label={t('events.form.gamesToWin')} error={errors.scoring?.gamesToWin?.message}>
            <input
              type="number"
              min={1}
              max={3}
              className={`${inputClass} tabular-nums`}
              {...register('scoring.gamesToWin', { valueAsNumber: true })}
            />
          </Field>
          <Field label={t('events.form.pointsToWin')} error={errors.scoring?.pointsToWin?.message}>
            <input
              type="number"
              min={1}
              max={50}
              className={`${inputClass} tabular-nums`}
              {...register('scoring.pointsToWin', { valueAsNumber: true })}
            />
          </Field>
          <Field label={t('events.form.pointCap')} error={errors.scoring?.pointCap?.message}>
            <input
              type="number"
              min={1}
              max={50}
              className={`${inputClass} tabular-nums`}
              {...register('scoring.pointCap', { valueAsNumber: true })}
            />
          </Field>
        </div>
      </fieldset>

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
