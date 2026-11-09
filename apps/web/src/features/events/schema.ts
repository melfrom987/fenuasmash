import { z } from 'zod';
import type { Event, ScoringRules } from '@/api/models';
import { key, type TranslationKey } from '@/i18n';
import { EVENT_FORMATS, EVENT_TYPES } from './constants';

const intBetween = (min: number, max: number, rangeKey: TranslationKey) =>
  z
    .number(key('events.form.errors.numberExpected'))
    .int(key('events.form.errors.integerExpected'))
    .min(min, rangeKey)
    .max(max, rangeKey);

// Les champs de poule restent dans le formulaire même masqués :
// on accepte NaN et on ne les vérifie que pour le format "poules".
const optionalNumber = z.union([z.number(), z.nan()]);

export const eventFormSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, key('events.form.errors.nameRequired'))
      .max(100, key('events.form.errors.nameTooLong')),
    type: z.enum(EVENT_TYPES, key('events.form.errors.typeRequired')),
    format: z.enum(EVENT_FORMATS),
    poolSize: optionalNumber,
    qualifiersPerPool: optionalNumber,
    scoring: z.object({
      gamesToWin: intBetween(1, 3, key('events.form.errors.gamesToWinRange')),
      pointsToWin: intBetween(1, 50, key('events.form.errors.pointsRange')),
      pointCap: intBetween(1, 50, key('events.form.errors.pointsRange')),
    }),
  })
  .superRefine((data, ctx) => {
    if (data.scoring.pointCap < data.scoring.pointsToWin) {
      ctx.addIssue({
        code: 'custom',
        path: ['scoring', 'pointCap'],
        message: key('events.form.errors.pointCapBelowPoints'),
      });
    }
    if (data.format !== 'POOLS_THEN_ELIMINATION') return;

    const { poolSize, qualifiersPerPool } = data;
    if (!Number.isInteger(poolSize) || poolSize < 3 || poolSize > 8) {
      ctx.addIssue({
        code: 'custom',
        path: ['poolSize'],
        message: key('events.form.errors.poolSizeRange'),
      });
    }
    if (
      !Number.isInteger(qualifiersPerPool) ||
      qualifiersPerPool < 1 ||
      qualifiersPerPool >= poolSize
    ) {
      ctx.addIssue({
        code: 'custom',
        path: ['qualifiersPerPool'],
        message: key('events.form.errors.qualifiersRange'),
      });
    }
  });

export type EventFormValues = z.infer<typeof eventFormSchema>;

/** Corps envoyé à l'API : les champs de poule seulement pour le format "poules" */
export type EventInput = {
  name: string;
  type: EventFormValues['type'];
  format: EventFormValues['format'];
  poolSize?: number;
  qualifiersPerPool?: number;
  scoring: ScoringRules;
};

export function toEventInput(values: EventFormValues): EventInput {
  const { poolSize, qualifiersPerPool, ...rest } = values;
  return values.format === 'POOLS_THEN_ELIMINATION'
    ? { ...rest, poolSize, qualifiersPerPool }
    : rest;
}

export function toEventFormValues(event: Event): EventFormValues {
  return {
    name: event.name,
    type: event.type,
    format: event.format,
    poolSize: event.poolSize ?? 4,
    qualifiersPerPool: event.qualifiersPerPool ?? 2,
    scoring: event.scoring,
  };
}
