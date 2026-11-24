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

const CATEGORY_MAX_AGE: [RegExp, number][] = [
  [/minibad/, 8],
  [/poussin/, 10],
  [/benjamin/, 12],
  [/minime/, 14],
  [/cadet/, 16],
  [/junior/, 18],
];

export type BirthYearBounds = { minBirthYear: number | null; maxBirthYear: number | null };

export function categoryBirthYears(label: string, referenceYear: number): BirthYearBounds | null {
  const value = label
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

  const under = /\bu\s?(\d{1,2})\b/.exec(value);
  if (under) return { minBirthYear: referenceYear - (Number(under[1]) - 1), maxBirthYear: null };

  for (const [pattern, maxAge] of CATEGORY_MAX_AGE) {
    if (pattern.test(value)) return { minBirthYear: referenceYear - maxAge, maxBirthYear: null };
  }

  if (/veteran/.test(value)) {
    const level = /\bv\s?([1-8])\b/.exec(value);
    const minAge = VETERAN_MIN_AGE + (level ? (Number(level[1]) - 1) * 5 : 0);
    return { minBirthYear: null, maxBirthYear: referenceYear - minAge };
  }

  if (/senior/.test(value)) return { minBirthYear: null, maxBirthYear: null };
  return null;
}

export function birthYearFits(bounds: BirthYearBounds, birthDate: string) {
  const year = Number(birthDate.slice(0, 4));
  return (
    (bounds.minBirthYear === null || year >= bounds.minBirthYear) &&
    (bounds.maxBirthYear === null || year <= bounds.maxBirthYear)
  );
}
