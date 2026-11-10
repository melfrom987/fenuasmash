import { z } from 'zod';
import type { Discipline, DrawFormat, ScoringRules, Tournament } from '@/api/models';
import { DISCIPLINES } from '@/features/events/constants';
import { key, type TranslationKey } from '@/i18n';

export const DRAW_FORMATS = [
  'SINGLE_ELIMINATION',
  'POOLS_THEN_ELIMINATION',
  'SINGLE_POOL',
] as const satisfies readonly DrawFormat[];

const intBetween = (min: number, max: number, rangeKey: TranslationKey) =>
  z
    .number(key('rules.errors.numberExpected'))
    .int(key('rules.errors.integerExpected'))
    .min(min, rangeKey)
    .max(max, rangeKey);

export const rulesSchema = z
  .object({
    drawFormat: z.enum(DRAW_FORMATS),
    scoring: z.object({
      gamesToWin: intBetween(1, 3, key('rules.errors.gamesToWinRange')),
      pointsToWin: intBetween(1, 50, key('rules.errors.pointsRange')),
      twoPointLead: z.boolean(),
      // Champ masqué quand twoPointLead est décoché : NaN accepté, vérifié plus bas
      pointCap: z.union([z.number(), z.nan()]),
    }),
    disciplines: z.array(z.enum(DISCIPLINES)).min(1, key('rules.errors.disciplinesRequired')),
  })
  .superRefine((data, ctx) => {
    const { twoPointLead, pointCap, pointsToWin } = data.scoring;
    if (!twoPointLead) return;
    if (!Number.isInteger(pointCap) || pointCap < pointsToWin || pointCap > 50) {
      ctx.addIssue({
        code: 'custom',
        path: ['scoring', 'pointCap'],
        message: key('rules.errors.pointCapRange'),
      });
    }
  });

export type RulesValues = z.infer<typeof rulesSchema>;

/** Formulaire de l'onglet Paramètres : uniquement les règles */
export const rulesFormSchema = z.object({ rules: rulesSchema });

/** Règles officielles : poules puis sortie, 2 sets gagnants de 21, plafond à 30 */
export const DEFAULT_RULES: RulesValues = {
  drawFormat: 'POOLS_THEN_ELIMINATION',
  scoring: { gamesToWin: 2, pointsToWin: 21, twoPointLead: true, pointCap: 30 },
  disciplines: ['MS', 'WS', 'MD', 'WD', 'XD'],
};

/** Corps envoyé à l'API */
export type RulesInput = {
  competitionType: 'INDIVIDUAL';
  playMode: 'SETS';
  drawFormat: DrawFormat;
  scoring: ScoringRules;
  disciplines: Discipline[];
};

export function toRulesInput(values: RulesValues): RulesInput {
  const { twoPointLead, pointCap } = values.scoring;
  return {
    competitionType: 'INDIVIDUAL',
    playMode: 'SETS',
    drawFormat: values.drawFormat,
    scoring: { ...values.scoring, pointCap: twoPointLead ? pointCap : null },
    disciplines: values.disciplines,
  };
}

/** Règles d'un tournoi existant ; valeurs officielles si le back ne les renvoie pas encore */
export function toRulesValues(tournament: Tournament): RulesValues {
  return {
    drawFormat: tournament.drawFormat ?? DEFAULT_RULES.drawFormat,
    scoring: tournament.scoring
      ? { ...tournament.scoring, pointCap: tournament.scoring.pointCap ?? Number.NaN }
      : DEFAULT_RULES.scoring,
    disciplines: tournament.disciplines?.length
      ? tournament.disciplines
      : DEFAULT_RULES.disciplines,
  };
}
