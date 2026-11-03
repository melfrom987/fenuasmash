import { key } from '@/i18n';
import { z } from 'zod';

export const tournamentFormSchema = z
  .object({
    organizationId: z.uuid('Choisissez une organisation'),
    name: z.string().trim().min(1, 'Le nom est obligatoire'),
    startsAt: z.string().min(1, 'La date de début est obligatoire'),
    endsAt: z.string().min(1, 'La date de fin est obligatoire'),
    minimumRestMinutes: z
      .number('Indiquez un nombre')
      .int('Nombre entier attendu')
      .min(0, 'Doit être positif'),
    courtCount: z
      .number(key('tournaments.form.errors.numberExpected'))
      .int(key('tournaments.form.errors.integerExpected'))
      .min(1, key('tournaments.form.errors.courtCountRange'))
      .max(50, key('tournaments.form.errors.courtCountRange')),
  })
  .refine((data) => data.endsAt > data.startsAt, {
    message: 'La fin doit être après le début',
    path: ['endsAt'],
  });

export type TournamentFormValues = z.infer<typeof tournamentFormSchema>;
