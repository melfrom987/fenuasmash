/**
 * Catégorie d'âge, calculée depuis l'année de naissance (jamais stockée).
 * Règle décrite dans docs/competition-rules.md (« Age Categories ») :
 * le backend applique la même.
 */

/** Mois de début de saison (9 = septembre). À confirmer avec la fédération. */
export const SEASON_START_MONTH = 9;

export const VETERAN_MIN_AGE = 35;

// Âge maximal (dans l'année de référence) de chaque catégorie jeune
const YOUTH_CATEGORIES = [
  [8, 'MINIBAD'],
  [10, 'POUSSIN'],
  [12, 'BENJAMIN'],
  [14, 'MINIME'],
  [16, 'CADET'],
  [18, 'JUNIOR'],
] as const;

type YouthCategory = (typeof YOUTH_CATEGORIES)[number][1];

export type AgeCategory =
  | { code: YouthCategory | 'SENIOR' }
  /** Vétéran V1 (35-39 ans) à V8 (70 ans et plus) */
  | { code: 'VETERAN'; level: number };

/** Une saison porte le nom de l'année où elle se termine : 2026-2027 → 2027 */
export function seasonReferenceYear(date = new Date()): number {
  const year = date.getFullYear();
  if (SEASON_START_MONTH === 1) return year;
  return date.getMonth() + 1 >= SEASON_START_MONTH ? year + 1 : year;
}

export function getAgeCategory(
  birthDate: string,
  referenceYear = seasonReferenceYear(),
): AgeCategory {
  const age = referenceYear - Number(birthDate.slice(0, 4));

  for (const [maxAge, code] of YOUTH_CATEGORIES) {
    if (age <= maxAge) return { code };
  }
  if (age < VETERAN_MIN_AGE) return { code: 'SENIOR' };
  return { code: 'VETERAN', level: Math.min(8, Math.floor((age - VETERAN_MIN_AGE) / 5) + 1) };
}
