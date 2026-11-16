export const SEASON_START_MONTH = 9;

export const VETERAN_MIN_AGE = 35;

const YOUTH_CATEGORIES = [
  [8, 'MINIBAD'],
  [10, 'POUSSIN'],
  [12, 'BENJAMIN'],
  [14, 'MINIME'],
  [16, 'CADET'],
  [18, 'JUNIOR'],
] as const;

type YouthCategory = (typeof YOUTH_CATEGORIES)[number][1];

export type AgeCategory = { code: YouthCategory | 'SENIOR' } | { code: 'VETERAN'; level: number };

export function seasonReferenceYear(date = new Date()): number {
  const year = date.getFullYear();
  const seasonEndsNextYear = SEASON_START_MONTH > 1 && date.getMonth() + 1 >= SEASON_START_MONTH;
  return seasonEndsNextYear ? year + 1 : year;
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
