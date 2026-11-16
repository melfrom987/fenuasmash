import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { key } from '@/i18n';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import type { LogoChange } from '../logo';
import { clubFormSchema, type ClubFormValues } from '../schema';
import { LogoInput } from './LogoInput';

const sectionClass = 'space-y-4 rounded-lg bg-white p-6 shadow-sm';
const sectionTitleClass = 'font-display text-base font-semibold';

export function ClubForm({
  defaultValues,
  currentLogoUrl = null,
  onSubmit,
}: {
  /** Absent = création ; présent = modification */
  defaultValues?: ClubFormValues;
  currentLogoUrl?: string | null;
  onSubmit: (values: ClubFormValues, logo: LogoChange) => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const [formError, setFormError] = useState<Error | null>(null);
  const [logo, setLogo] = useState<LogoChange>({ type: 'keep' });
  const isCreation = !defaultValues;

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ClubFormValues>({
    resolver: zodResolver(clubFormSchema),
    defaultValues: defaultValues ?? { name: '', city: '', address: '', email: '', phone: '' },
  });
  const name = useWatch({ control, name: 'name' });

  const submit = async (values: ClubFormValues) => {
    setFormError(null);
    try {
      await onSubmit(values, logo);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
        CLUB_NAME_ALREADY_EXISTS: { field: 'name', message: key('clubs.form.errors.nameTaken') },
      });
      if (!shownOnField) setFormError(error instanceof Error ? error : new Error(String(error)));
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} noValidate className="space-y-6">
      {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={sectionClass}>
          <h2 className={sectionTitleClass}>{t('clubs.form.details')}</h2>
          <Field label={t('clubs.form.name')} error={errors.name?.message}>
            <input className={inputClass} autoComplete="off" {...register('name')} />
          </Field>
          <Field label={t('clubs.form.city')} error={errors.city?.message}>
            <input className={inputClass} {...register('city')} />
          </Field>
          <Field
            label={t('clubs.form.address')}
            error={errors.address?.message}
            hint={t('clubs.form.addressHint')}
          >
            <textarea rows={2} className={inputClass} {...register('address')} />
          </Field>
        </section>

        <div className="space-y-6">
          <section className={sectionClass}>
            <h2 className={sectionTitleClass}>{t('clubs.form.logo')}</h2>
            <LogoInput
              clubName={name}
              currentUrl={currentLogoUrl}
              value={logo}
              onChange={setLogo}
            />
          </section>

          <section className={sectionClass}>
            <h2 className={sectionTitleClass}>{t('clubs.form.contact')}</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={t('clubs.form.email')} error={errors.email?.message}>
                <input type="email" className={inputClass} {...register('email')} />
              </Field>
              <Field label={t('clubs.form.phone')} error={errors.phone?.message}>
                <input type="tel" className={inputClass} {...register('phone')} />
              </Field>
            </div>
          </section>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting
            ? t('clubs.form.saving')
            : isCreation
              ? t('clubs.form.create')
              : t('common.save')}
        </Button>
      </div>
    </form>
  );
}
