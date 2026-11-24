import type { CategoryType, Discipline, Gender } from '@/api/models';
import { normalizeText } from '@/lib/text';

export type Family = 'SINGLES' | 'DOUBLES' | 'MIXED';

const words = (text: string) =>
  normalizeText(text)
    .split(/[^a-z0-9]+/)
    .filter(Boolean);

const MALE_WORDS = new Set([
  'h',
  'g',
  'm',
  'homme',
  'hommes',
  'garcon',
  'garcons',
  'messieurs',
  'masculin',
  'man',
  'men',
  'boy',
  'boys',
]);
const FEMALE_WORDS = new Set([
  'f',
  'dame',
  'dames',
  'femme',
  'femmes',
  'fille',
  'filles',
  'feminin',
  'woman',
  'women',
  'girl',
  'girls',
]);

export function valueGender(text: string): Gender | undefined {
  for (const word of words(text)) {
    const suffix = /^u\d+([gfhm])$/.exec(word);
    if (suffix) return suffix[1] === 'f' ? 'FEMALE' : 'MALE';
    if (MALE_WORDS.has(word)) return 'MALE';
    if (FEMALE_WORDS.has(word) || /(ette|ine)$/.test(word)) return 'FEMALE';
  }
  return undefined;
}

export function disciplineFamily(text: string): Family | undefined {
  const value = normalizeText(text);
  if (/mixte|mixed/.test(value)) return 'MIXED';
  if (/double/.test(value)) return 'DOUBLES';
  if (/simple|single/.test(value)) return 'SINGLES';
  return undefined;
}

export function fitsGender(discipline: Discipline, gender: Gender | undefined) {
  if (!gender) return true;
  if (discipline === 'MS' || discipline === 'MD') return gender === 'MALE';
  if (discipline === 'WS' || discipline === 'WD') return gender === 'FEMALE';
  return true;
}

export function fitsFamily(discipline: Discipline, family: Family | undefined) {
  if (!family) return true;
  if (family === 'SINGLES') return discipline === 'MS' || discipline === 'WS';
  if (family === 'MIXED') return discipline === 'XD';
  return discipline === 'MD' || discipline === 'WD' || discipline === 'OD';
}

export function guessDiscipline(text: string, header: string): Discipline {
  const family = disciplineFamily(text) ?? disciplineFamily(header);
  const gender = valueGender(text);
  if (family === 'MIXED') return 'XD';
  if (family === 'DOUBLES') return gender === 'FEMALE' ? 'WD' : gender === 'MALE' ? 'MD' : 'OD';
  return gender === 'FEMALE' ? 'WS' : 'MS';
}

const AGE_CATEGORIES: [RegExp, string, string][] = [
  [/^minibad/, 'Minibad', 'U9'],
  [/^poussin/, 'Poussin', 'U11'],
  [/^benjamin/, 'Benjamin', 'U13'],
  [/^minime/, 'Minime', 'U15'],
  [/^cadet/, 'Cadet', 'U17'],
  [/^junior/, 'Junior', 'U19'],
  [/^senior/, 'Sénior', ''],
  [/^veteran/, 'Vétéran', ''],
];

export function guessCategory(text: string): { categoryType: CategoryType; categoryLabel: string } {
  const list = words(text);
  for (const word of list) {
    for (const [pattern, label, under] of AGE_CATEGORIES) {
      if (!pattern.test(word)) continue;
      const level = list.find((w) => /^v[1-8]$/.test(w));
      const suffix = label === 'Vétéran' && level ? level.toUpperCase() : under;
      return { categoryType: 'AGE', categoryLabel: suffix ? `${label} ${suffix}` : label };
    }
  }
  return { categoryType: 'OPEN', categoryLabel: '' };
}
