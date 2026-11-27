import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import type { Tournament } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { useErrorMessage } from '@/lib/use-error-message';
import { useUpdateTournament } from '../api';
import {
  matchesPerHour,
  PUBLIC_VISIBILITIES,
  scheduleSettingsSchema,
  toScheduleSettingsValues,
  type ScheduleSettingsValues,
} from '../schedule-settings';

export function ScheduleSettingsForm({
  tournament,
  activeCourts,
}: {
  tournament: Tournament;
  activeCourts: number;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const updateTournament = useUpdateTournament(tournament.id);
  const [saved, setSaved] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ScheduleSettingsValues>({
    resolver: zodResolver(scheduleSettingsSchema),
    defaultValues: toScheduleSettingsValues(tournament),
  });
  const average = useWatch({ control, name: 'averageMatchMinutes' });
  const perHour = matchesPerHour(activeCourts, Number.isFinite(average) ? average : 0);

  const submit = async (values: ScheduleSettingsValues) => {
    setSaved(false);
    const updated = await updateTournament.mutateAsync(values);
    reset(toScheduleSettingsValues(updated));
    setSaved(true);
  };

  const minutesInput = (name: Exclude<keyof ScheduleSettingsValues, 'publicVisibility'>) => (
    <div className="flex items-center gap-2">
      <input
        type="number"
        min={0}
        max={120}
        className={`${inputClass} mt-0 w-24 tabular-nums`}
        {...register(name, { valueAsNumber: true })}
      />
      <span className="text-sable-600 text-sm">{t('schedule.minutes')}</span>
    </div>
  );

  return (
    <form
      onSubmit={handleSubmit(submit)}
      noValidate
      className="space-y-5 rounded-lg bg-white p-6 shadow-sm"
    >
      {updateTournament.error && (
        <ErrorMessage>{errorMessage(updateTournament.error)}</ErrorMessage>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Field
          label={t('schedule.averageMatch')}
          error={errors.averageMatchMinutes?.message}
          hint={t('schedule.averageMatchHint')}
        >
          {minutesInput('averageMatchMinutes')}
        </Field>
        <Field
          label={t('schedule.rest')}
          error={errors.minimumRestMinutes?.message}
          hint={t('schedule.restHint')}
        >
          {minutesInput('minimumRestMinutes')}
        </Field>
        <Field
          label={t('schedule.convocation')}
          error={errors.convocationLeadMinutes?.message}
          hint={t('schedule.convocationHint')}
        >
          {minutesInput('convocationLeadMinutes')}
        </Field>
      </div>

      {perHour > 0 && (
        <p className="bg-sable-50 text-sable-700 rounded-md px-3 py-2 text-sm">
          {t('schedule.capacity', { courts: activeCourts, count: perHour })}
        </p>
      )}

      <fieldset className="space-y-2">
        <legend className="text-sable-800 text-sm font-medium">{t('schedule.visibility')}</legend>
        {PUBLIC_VISIBILITIES.map((value) => (
          <label key={value} className="flex items-start gap-2 text-sm">
            <input type="radio" value={value} className="mt-1" {...register('publicVisibility')} />
            <span>
              <span className="font-medium">{t(`schedule.visibilities.${value}.label`)}</span>
              <span className="text-sable-600 block">
                {t(`schedule.visibilities.${value}.hint`)}
              </span>
            </span>
          </label>
        ))}
      </fieldset>

      <div className="flex items-center gap-3">
        <Button type="submit" disabled={isSubmitting || !isDirty}>
          {isSubmitting ? t('rules.saving') : t('rules.save')}
        </Button>
        {saved && !isDirty && <span className="text-lagon-700 text-sm">{t('rules.saved')}</span>}
      </div>
    </form>
  );
}
