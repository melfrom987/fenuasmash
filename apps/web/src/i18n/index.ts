import i18n, { type ParseKeys } from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import { en } from './locales/en';
import { fr } from './locales/fr';

export const supportedLanguages = ['fr', 'en'] as const;
export type Language = (typeof supportedLanguages)[number];

// Une clé de traduction valide, ex. 'tournaments.title'
export type TranslationKey = ParseKeys;

// Pour les messages des schémas Zod : vérifie que la clé existe
export const key = (k: TranslationKey) => k;

export const resources = {
  fr: { translation: fr },
  en: { translation: en },
};

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'fr',
    supportedLngs: [...supportedLanguages],
    load: 'languageOnly',
    interpolation: { escapeValue: false },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  });

document.documentElement.lang = i18n.resolvedLanguage ?? 'fr';
i18n.on('languageChanged', (lng) => {
  document.documentElement.lang = lng;
});

export default i18n;
