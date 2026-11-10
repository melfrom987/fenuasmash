import { z } from 'zod';
import { key } from '@/i18n';
import { rulesSchema } from './rules';

export const tournamentFormSchema = z
  .object({
    organizationId: z.uuid(key('tournaments.form.errors.organizationRequired')),
    name: z
      .string()
      .trim()
      .min(1, key('tournaments.form.errors.nameRequired'))
      .max(200, key('tournaments.form.errors.nameTooLong')),
    startsAt: z.string().min(1, key('tournaments.form.errors.startsAtRequired')),
    endsAt: z.string().min(1, key('tournaments.form.errors.endsAtRequired')),
    minimumRestMinutes: z
      .number(key('tournaments.form.errors.numberExpected'))
      .int(key('tournaments.form.errors.integerExpected'))
      .min(0, key('tournaments.form.errors.restRange'))
      .max(1440, key('tournaments.form.errors.restRange')),
    courtCount: z
      .number(key('tournaments.form.errors.numberExpected'))
      .int(key('tournaments.form.errors.integerExpected'))
      .min(1, key('tournaments.form.errors.courtCountRange'))
      .max(50, key('tournaments.form.errors.courtCountRange')),
    rules: rulesSchema,
  })
  .refine((data) => data.endsAt > data.startsAt, {
    message: key('tournaments.form.errors.endsBeforeStart'),
    path: ['endsAt'],
  });

export type TournamentFormValues = z.infer<typeof tournamentFormSchema>;
