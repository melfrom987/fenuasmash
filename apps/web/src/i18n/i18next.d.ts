import 'i18next';
import type { Translation } from './locales/fr';

declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation';
    resources: { translation: Translation };
  }
}
