import { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import type { Tournament } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { key } from '@/i18n';
import { applyApiFieldErrors } from '@/lib/form-errors';
import { useErrorMessage } from '@/lib/use-error-message';
import { useUpdateTournament } from '../api';
import { rulesFormSchema, toRulesInput, toRulesValues, type RulesValues } from '../rules';
import { RulesFields } from './RulesFields';

export function RulesForm({ tournament }: { tournament: Tournament }) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const updateTournament = useUpdateTournament(tournament.id);
  const [formError, setFormError] = useState<Error | null>(null);
  const [saved, setSaved] = useState(false);

  const form = useForm<{ rules: RulesValues }>({
    resolver: zodResolver(rulesFormSchema),
    defaultValues: { rules: toRulesValues(tournament) },
  });
  const {
    handleSubmit,
    setError,
    reset,
    formState: { isSubmitting, isDirty },
  } = form;

  const submit = async ({ rules }: { rules: RulesValues }) => {
    setFormError(null);
    setSaved(false);
    try {
      const updated = await updateTournament.mutateAsync(toRulesInput(rules));
      reset({ rules: toRulesValues(updated) });
      setSaved(true);
    } catch (error) {
      const shownOnField = applyApiFieldErrors(error, setError, {
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
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-5">
        {formError && <ErrorMessage>{errorMessage(formError)}</ErrorMessage>}
        <RulesFields />
        <div className="flex items-center gap-3">
          <Button type="submit" disabled={isSubmitting || !isDirty}>
            {isSubmitting ? t('rules.saving') : t('rules.save')}
          </Button>
          {saved && !isDirty && <span className="text-lagon-700 text-sm">{t('rules.saved')}</span>}
        </div>
      </form>
    </FormProvider>
  );
}
