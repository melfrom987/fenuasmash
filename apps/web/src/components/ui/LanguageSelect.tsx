import { useTranslation } from 'react-i18next';
import { supportedLanguages, type Language } from '@/i18n';
import { inputClass } from './styles';

const LANGUAGE_NAMES: Record<Language, string> = { fr: 'Français', en: 'English' };

export function LanguageSelect({ selectClassName = inputClass }: { selectClassName?: string }) {
  const { t, i18n } = useTranslation();
  const current = (i18n.resolvedLanguage ?? 'fr') as Language;

  return (
    <label className="block">
      <span className="sr-only">{t('common.language')}</span>
      <select
        value={current}
        onChange={(e) => void i18n.changeLanguage(e.target.value)}
        className={selectClassName}
      >
        {supportedLanguages.map((language) => (
          <option key={language} value={language} lang={language}>
            {LANGUAGE_NAMES[language]}
          </option>
        ))}
      </select>
    </label>
  );
}
