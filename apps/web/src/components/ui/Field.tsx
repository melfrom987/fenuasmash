import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { TranslationKey } from '@/i18n';

export function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string;
  /** Clé de traduction du message d'erreur */
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  const { t } = useTranslation();

  return (
    <label className="block">
      <span className="text-sable-800 text-sm font-medium">{label}</span>
      {children}
      {hint && !error && <span className="text-sable-600 mt-1 block text-xs">{hint}</span>}
      {error && (
        <span className="mt-1 block text-sm text-red-700">{t(error as TranslationKey)}</span>
      )}
    </label>
  );
}
