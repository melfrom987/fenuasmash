import { key, type TranslationKey } from '@/i18n';

export const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
export const LOGO_MAX_BYTES = 1024 * 1024;

export type LogoChange =
  { type: 'keep' } | { type: 'replace'; file: File; previewUrl: string } | { type: 'remove' };

export const readAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error ?? new Error('Cannot read file'));
    reader.readAsDataURL(file);
  });

export function validateLogo(file: File): TranslationKey | null {
  if (!LOGO_TYPES.includes(file.type)) return key('clubs.form.errors.logoType');
  if (file.size > LOGO_MAX_BYTES) return key('clubs.form.errors.logoSize');
  return null;
}
