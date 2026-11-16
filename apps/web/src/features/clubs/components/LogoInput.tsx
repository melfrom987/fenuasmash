import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { secondaryButtonClass } from '@/components/ui/styles';
import { key, type TranslationKey } from '@/i18n';
import { LOGO_TYPES, readAsDataUrl, validateLogo, type LogoChange } from '../logo';
import { ClubLogo } from './ClubLogo';

export function LogoInput({
  clubName,
  currentUrl,
  value,
  onChange,
}: {
  clubName: string;
  /** Logo déjà enregistré (modification), ou null */
  currentUrl: string | null;
  value: LogoChange;
  onChange: (change: LogoChange) => void;
}) {
  const { t } = useTranslation();
  const inputId = useId();
  const [error, setError] = useState<TranslationKey | null>(null);
  const shownUrl =
    value.type === 'replace' ? value.previewUrl : value.type === 'remove' ? null : currentUrl;

  const handleFile = async (selected: File | undefined) => {
    if (!selected) return;
    const problem = validateLogo(selected);
    setError(problem);
    if (problem) return;
    try {
      onChange({ type: 'replace', file: selected, previewUrl: await readAsDataUrl(selected) });
    } catch {
      setError(key('clubs.form.errors.logoType'));
    }
  };

  return (
    <div className="flex items-center gap-4">
      <ClubLogo club={{ name: clubName || '?', logoUrl: shownUrl }} size="lg" />
      <div className="space-y-2">
        <div className="flex flex-wrap gap-2">
          <label htmlFor={inputId} className={`${secondaryButtonClass} cursor-pointer`}>
            {shownUrl ? t('clubs.form.changeLogo') : t('clubs.form.chooseLogo')}
          </label>
          <input
            id={inputId}
            type="file"
            accept={LOGO_TYPES.join(',')}
            className="sr-only"
            onChange={(e) => {
              void handleFile(e.target.files?.[0]);
              e.target.value = ''; // permet de rechoisir le même fichier
            }}
          />
          {shownUrl && (
            <button
              type="button"
              onClick={() => {
                setError(null);
                onChange(currentUrl ? { type: 'remove' } : { type: 'keep' });
              }}
              className="rounded-md px-3 py-2 text-sm text-red-700 hover:bg-red-50"
            >
              {t('clubs.form.removeLogo')}
            </button>
          )}
        </div>
        {error ? (
          <p className="text-sm text-red-700">{t(error)}</p>
        ) : (
          <p className="text-sable-600 text-xs">{t('clubs.form.logoHint')}</p>
        )}
      </div>
    </div>
  );
}
