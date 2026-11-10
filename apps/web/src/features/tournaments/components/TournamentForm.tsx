import { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import type { Organization } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { key } from '@/i18n';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import { DEFAULT_RULES } from '../rules';
import { tournamentFormSchema, type TournamentFormValues } from '../schema';
import { RulesFields } from './RulesFields';

export function TournamentForm({
  organizations,
  onSubmit,
}: {
  organizations: Organization[];
  onSubmit: (values: TournamentFormValues) => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const [formError, setFormError] = useState<Error | null>(null);

  const form = useForm<TournamentFormValues>({
    resolver: zodResolver(tournamentFormSchema),
    defaultValues: {
      organizationId: organizations[0]?.id ?? '',
      name: '',
      startsAt: '',
      endsAt: '',
      minimumRestMinutes: 20,
      courtCount: 4,
      rules: DEFAULT_RULES,
    },
  });
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = form;

  const submit = async (values: TournamentFormValues) => {
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
        INVALID_DATE_RANGE: {
          field: 'endsAt',
          message: key('tournaments.form.errors.endsBeforeStart'),
        },
        INVALID_ORGANIZATION: {
          field: 'organizationId',
          message: key('tournaments.form.errors.organizationRequired'),
        },
        INVALID_SCORING_RULES: {
          field: 'rules.scoring.pointCap',
          message: key('rules.errors.pointCapRange'),
        },
      });
      if (!shownOnField) setFormError(error instanceof Error ? error : new Error(String(error)));
    }
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-6">
        {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}

        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <section className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
            <h2 className="font-display text-lg font-semibold">{t('tournaments.form.general')}</h2>

            <Field
              label={t('tournaments.form.organization')}
              error={errors.organizationId?.message}
            >
              <select className={inputClass} {...register('organizationId')}>
                {organizations.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.name}
                  </option>
                ))}
              </select>
            </Field>

            <Field label={t('tournaments.form.name')} error={errors.name?.message}>
              <input
                className={inputClass}
                placeholder={t('tournaments.form.namePlaceholder')}
                {...register('name')}
              />
            </Field>

            <Field label={t('tournaments.form.startsAt')} error={errors.startsAt?.message}>
              <input type="datetime-local" className={inputClass} {...register('startsAt')} />
            </Field>
            <Field label={t('tournaments.form.endsAt')} error={errors.endsAt?.message}>
              <input type="datetime-local" className={inputClass} {...register('endsAt')} />
            </Field>

            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                label={t('tournaments.form.minimumRest')}
                error={errors.minimumRestMinutes?.message}
                hint={t('tournaments.form.minimumRestHint')}
              >
                <input
                  type="number"
                  min={0}
                  max={1440}
                  className={`${inputClass} tabular-nums`}
                  {...register('minimumRestMinutes', { valueAsNumber: true })}
                />
              </Field>
              <Field label={t('tournaments.form.courtCount')} error={errors.courtCount?.message}>
                <input
                  type="number"
                  min={1}
                  max={50}
                  className={`${inputClass} tabular-nums`}
                  {...register('courtCount', { valueAsNumber: true })}
                />
              </Field>
            </div>
          </section>

          <section className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
            <div>
              <h2 className="font-display text-lg font-semibold">{t('rules.title')}</h2>
              <p className="text-sable-600 text-sm">{t('rules.hint')}</p>
            </div>
            <RulesFields />
          </section>
        </div>

        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? t('tournaments.form.submitting') : t('tournaments.form.submit')}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
}
