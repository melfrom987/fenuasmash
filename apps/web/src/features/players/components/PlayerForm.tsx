import { useState } from 'react';
import { Link } from 'react-router';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { inputClass } from '@/components/ui/styles';
import { useClubs } from '@/features/clubs/api';
import { key, type TranslationKey } from '@/i18n';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import { seasonReferenceYear } from '../age-category';
import { GENDERS, RANKING_DISCIPLINES } from '../constants';
import { emptyRankingFormValues, playerFormSchema, type PlayerFormValues } from '../schema';
import { AgeCategoryBadge } from './AgeCategoryBadge';

const sectionClass = 'space-y-4 rounded-lg bg-white p-6 shadow-sm';
const sectionTitleClass = 'font-display text-base font-semibold';

export function PlayerForm({
  defaultValues,
  onSubmit,
}: {
  /** Absent = création ; présent = modification */
  defaultValues?: PlayerFormValues;
  onSubmit: (values: PlayerFormValues) => Promise<unknown>;
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const clubs = useClubs();
  const [formError, setFormError] = useState<Error | null>(null);
  const isCreation = !defaultValues;

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<PlayerFormValues>({
    resolver: zodResolver(playerFormSchema),
    // Le genre n'a pas de valeur par défaut : l'organisateur doit le choisir
    defaultValues: defaultValues ?? {
      lastName: '',
      firstName: '',
      birthDate: '',
      clubId: '',
      licenseNumber: '',
      email: '',
      phone: '',
      rankings: emptyRankingFormValues(),
    },
  });

  // Catégorie affichée dès que la date de naissance est complète
  const birthDate = useWatch({ control, name: 'birthDate' });
  const hasBirthDate = /^\d{4}-\d{2}-\d{2}$/.test(birthDate ?? '');
  const referenceYear = seasonReferenceYear();

  const submit = async (values: PlayerFormValues) => {
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
        PLAYER_LICENSE_ALREADY_EXISTS: {
          field: 'licenseNumber',
          message: key('players.form.errors.licenseTaken'),
        },
        INVALID_CLUB: { field: 'clubId', message: key('players.form.errors.clubRequired') },
      });
      if (!shownOnField) setFormError(error instanceof Error ? error : new Error(String(error)));
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} noValidate className="space-y-6">
      {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className={sectionClass}>
          <h2 className={sectionTitleClass}>{t('players.form.identity')}</h2>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={t('players.form.lastName')} error={errors.lastName?.message}>
              <input className={inputClass} autoComplete="off" {...register('lastName')} />
            </Field>
            <Field label={t('players.form.firstName')} error={errors.firstName?.message}>
              <input className={inputClass} autoComplete="off" {...register('firstName')} />
            </Field>
          </div>

          <fieldset>
            <legend className="text-sable-800 text-sm font-medium">
              {t('players.form.gender')}
            </legend>
            <div className="mt-2 flex gap-6">
              {GENDERS.map((gender) => (
                <label key={gender} className="text-sable-800 flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    value={gender}
                    className="accent-lagon-600 size-4"
                    {...register('gender')}
                  />
                  {t(`players.genders.${gender}`)}
                </label>
              ))}
            </div>
            {errors.gender?.message && (
              <p className="mt-1 text-sm text-red-700">
                {t(errors.gender.message as TranslationKey)}
              </p>
            )}
          </fieldset>

          <Field label={t('players.form.birthDate')} error={errors.birthDate?.message}>
            <input type="date" className={`${inputClass} sm:max-w-56`} {...register('birthDate')} />
          </Field>
          {hasBirthDate && (
            <p className="text-sable-700 flex items-center gap-2 text-sm">
              {t('players.form.ageCategory', {
                season: `${referenceYear - 1}–${referenceYear}`,
              })}
              <AgeCategoryBadge birthDate={birthDate} />
            </p>
          )}
        </section>

        <div className="space-y-6">
          <section className={sectionClass}>
            <h2 className={sectionTitleClass}>{t('players.form.clubAndLicense')}</h2>

            <Field label={t('players.form.club')} error={errors.clubId?.message}>
              <select className={inputClass} {...register('clubId')}>
                <option value="">{t('players.form.chooseClub')}</option>
                {clubs.data?.map((club) => (
                  <option key={club.id} value={club.id}>
                    {club.name}
                  </option>
                ))}
              </select>
            </Field>
            <Link to="/admin/clubs" className="text-lagon-700 text-sm hover:underline">
              {t('players.form.manageClubs')}
            </Link>

            <Field
              label={t('players.form.licenseNumber')}
              error={errors.licenseNumber?.message}
              hint={t('players.form.licenseHint')}
            >
              <input className={inputClass} autoComplete="off" {...register('licenseNumber')} />
            </Field>
          </section>

          <section className={sectionClass}>
            <h2 className={sectionTitleClass}>{t('players.form.contact')}</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={t('players.form.email')} error={errors.email?.message}>
                <input type="email" className={inputClass} {...register('email')} />
              </Field>
              <Field label={t('players.form.phone')} error={errors.phone?.message}>
                <input type="tel" className={inputClass} {...register('phone')} />
              </Field>
            </div>
          </section>

          <section className={sectionClass}>
            <div>
              <h2 className={sectionTitleClass}>{t('players.form.ranking')}</h2>
              <p className="text-sable-600 text-sm">{t('players.form.rankingHint')}</p>
            </div>
            <table className="w-full">
              <thead>
                <tr className="text-sable-600 text-left text-xs font-medium tracking-wide uppercase">
                  <th className="pb-1 font-medium">
                    <span className="sr-only">{t('players.form.rankingDiscipline')}</span>
                  </th>
                  <th className="pb-1 pl-3 font-medium">{t('players.form.rankingPoints')}</th>
                  <th className="pb-1 pl-3 font-medium">{t('players.form.rankingPosition')}</th>
                </tr>
              </thead>
              <tbody>
                {RANKING_DISCIPLINES.map((discipline) => {
                  const rankingErrors = errors.rankings?.[discipline];
                  const label = t(`players.rankingDisciplines.${discipline}`);
                  return (
                    <tr key={discipline} className="align-top">
                      <th scope="row" className="text-sable-800 pt-3 text-left text-sm font-medium">
                        {label}
                      </th>
                      <td className="pl-3">
                        <Field label="" error={rankingErrors?.points?.message}>
                          <input
                            type="number"
                            min={0}
                            aria-label={`${label} – ${t('players.form.rankingPoints')}`}
                            className={`${inputClass} tabular-nums`}
                            {...register(`rankings.${discipline}.points`, { valueAsNumber: true })}
                          />
                        </Field>
                      </td>
                      <td className="pl-3">
                        <Field label="" error={rankingErrors?.position?.message}>
                          <input
                            type="number"
                            min={1}
                            aria-label={`${label} – ${t('players.form.rankingPosition')}`}
                            className={`${inputClass} tabular-nums`}
                            {...register(`rankings.${discipline}.position`, {
                              valueAsNumber: true,
                            })}
                          />
                        </Field>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </section>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting
            ? t('players.form.saving')
            : isCreation
              ? t('players.form.create')
              : t('common.save')}
        </Button>
      </div>
    </form>
  );
}
