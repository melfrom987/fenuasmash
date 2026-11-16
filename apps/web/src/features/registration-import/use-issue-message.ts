import { useTranslation } from 'react-i18next';
import type { Translation } from '@/i18n/locales/fr';
import type { ImportIssue } from './types';

type ApiErrorCode = Extract<keyof Translation['apiErrors'], string>;

export function useIssueMessage() {
  const { t, i18n } = useTranslation();
  return (issue: ImportIssue) =>
    i18n.exists(`apiErrors.${issue.code}`)
      ? t(`apiErrors.${issue.code as ApiErrorCode}`)
      : issue.message;
}
