import type { CategoryType, Discipline } from '@/api/models';

export const DISCIPLINES = [
  'MS',
  'WS',
  'MD',
  'WD',
  'XD',
  'OD',
] as const satisfies readonly Discipline[];

export const CATEGORY_TYPES = ['OPEN', 'AGE', 'LEVEL'] as const satisfies readonly CategoryType[];

export const AGE_CATEGORY_SUGGESTIONS = [
  'Poussin',
  'Benjamin',
  'Minime',
  'Cadet',
  'Junior',
  'Sénior',
  'Vétéran',
];
export const LEVEL_CATEGORY_SUGGESTIONS = ['Série 4', 'Série 3', 'Série 2', 'Série 1'];
