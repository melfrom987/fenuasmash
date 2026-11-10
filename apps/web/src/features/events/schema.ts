import { z } from 'zod';
import type { Event } from '@/api/models';
import { key } from '@/i18n';
import { isoToTahitiInput, tahitiInputToIso } from '@/lib/dates';
import { CATEGORY_TYPES, DISCIPLINES } from './constants';

export const eventFormSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, key('events.form.errors.nameRequired'))
      .max(100, key('events.form.errors.nameTooLong')),
    discipline: z.enum(DISCIPLINES, key('events.form.errors.disciplineRequired')),
    categoryType: z.enum(CATEGORY_TYPES),
    categoryLabel: z.string().trim().max(50, key('events.form.errors.categoryLabelTooLong')),
    plannedDate: z.string(), // '' = pas de date prévue
    maxEntries: z.union([z.number(), z.nan()]), // NaN (champ vide) = non limité
  })
  .superRefine((data, ctx) => {
    if (data.categoryType !== 'OPEN' && data.categoryLabel === '') {
      ctx.addIssue({
        code: 'custom',
        path: ['categoryLabel'],
        message: key('events.form.errors.categoryLabelRequired'),
      });
    }
    const max = data.maxEntries;
    if (!Number.isNaN(max) && (!Number.isInteger(max) || max < 2 || max > 256)) {
      ctx.addIssue({
        code: 'custom',
        path: ['maxEntries'],
        message: key('events.form.errors.maxEntriesRange'),
      });
    }
  });

export type EventFormValues = z.infer<typeof eventFormSchema>;

/** Corps envoyé à l'API (création et modification) */
export type EventInput = {
  name: string;
  discipline: EventFormValues['discipline'];
  categoryType: EventFormValues['categoryType'];
  categoryLabel: string | null;
  plannedDate: string | null; // ISO, UTC
  maxEntries: number | null;
};

export function toEventInput(values: EventFormValues): EventInput {
  return {
    name: values.name,
    discipline: values.discipline,
    categoryType: values.categoryType,
    categoryLabel: values.categoryType === 'OPEN' ? null : values.categoryLabel,
    plannedDate: values.plannedDate ? tahitiInputToIso(values.plannedDate) : null,
    maxEntries: Number.isNaN(values.maxEntries) ? null : values.maxEntries,
  };
}

export function toEventFormValues(event: Event): EventFormValues {
  return {
    name: event.name,
    discipline: event.discipline,
    categoryType: event.categoryType,
    categoryLabel: event.categoryLabel ?? '',
    plannedDate: event.plannedDate ? isoToTahitiInput(event.plannedDate) : '',
    maxEntries: event.maxEntries ?? Number.NaN,
  };
}
