import { HttpResponse } from 'msw';

export const problem = (
  status: number,
  code: string,
  detail: string,
  errors?: { field: string; message: string }[],
) => HttpResponse.json({ code, detail, status, errors }, { status });

export const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();
