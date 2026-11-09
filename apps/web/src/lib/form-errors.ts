import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { key, type TranslationKey } from '@/i18n';
import { ApiError } from './api-client';

/** Codes d'erreur métier rattachés à un champ précis du formulaire */
export type CodeToField<T extends FieldValues> = Record<
  string,
  { field: Path<T>; message: TranslationKey }
>;

/**
 * Place les erreurs de l'API sous les bons champs.
 * Renvoie true si l'erreur a été affichée sur un champ, false sinon
 * (à afficher alors comme erreur générale).
 */
export function applyApiFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  codeToField: CodeToField<T> = {},
): boolean {
  if (!(error instanceof ApiError)) return false;

  const mapped = error.code ? codeToField[error.code] : undefined;
  if (mapped) {
    setError(mapped.field, { message: mapped.message });
    return true;
  }

  if (error.code === 'VALIDATION_FAILED' && error.fieldErrors.length > 0) {
    for (const { field } of error.fieldErrors) {
      setError(field as Path<T>, { message: key('common.invalidField') });
    }
    return true;
  }

  return false;
}
