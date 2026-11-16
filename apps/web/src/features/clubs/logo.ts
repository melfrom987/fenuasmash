import { key, type TranslationKey } from '@/i18n';

/** Mêmes limites que le backend (PUT /clubs/{clubId}/logo) */
export const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
export const LOGO_MAX_BYTES = 1024 * 1024;

/** Ce que l'organisateur a fait du logo dans le formulaire */
export type LogoChange =
  | { type: 'keep' }
  | { type: 'replace'; file: File; previewUrl: string }
  | { type: 'remove' };

/** Lit l'image pour l'aperçu (data URL : rien à libérer ensuite) */
export const readAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error ?? new Error('Cannot read file'));
    reader.readAsDataURL(file);
  });

/** Renvoie la clé d'erreur à afficher, ou null si le fichier convient */
export function validateLogo(file: File): TranslationKey | null {
  if (!LOGO_TYPES.includes(file.type)) return key('clubs.form.errors.logoType');
  if (file.size > LOGO_MAX_BYTES) return key('clubs.form.errors.logoSize');
  return null;
}
