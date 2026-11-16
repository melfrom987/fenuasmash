import { HttpResponse } from 'msw';

/** Réponse d'erreur au format Problem Details, comme le backend */
export const problem = (
  status: number,
  code: string,
  detail: string,
  errors?: { field: string; message: string }[],
) => HttpResponse.json({ code, detail, status, errors }, { status });

/** Minuscules et sans accents : « Léa » et « lea » se retrouvent */
export const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();
