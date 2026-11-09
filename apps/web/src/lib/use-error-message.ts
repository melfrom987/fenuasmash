import { useTranslation } from 'react-i18next';
import type { Translation } from '@/i18n/locales/fr';
import { ApiError } from './api-client';

type ApiErrorCode = Extract<keyof Translation['apiErrors'], string>;

export function useErrorMessage() {
  const { t, i18n } = useTranslation();

  return (error: Error) => {
    if (error instanceof ApiError && error.code && i18n.exists(`apiErrors.${error.code}`)) {
      return t(`apiErrors.${error.code as ApiErrorCode}`);
    }
    return error.message;
  };
}
