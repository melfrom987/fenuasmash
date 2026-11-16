import { z } from 'zod';
import type { Gender, Player, PlayerRankings, Ranking } from '@/api/models';
import { key, type TranslationKey } from '@/i18n';
import { GENDERS } from './constants';

const today = () => new Date().toISOString().slice(0, 10);

// Champs numériques facultatifs : vide = NaN (valueAsNumber)
const optionalInt = (min: number, minKey: TranslationKey) =>
  z.union([z.number().int(key('players.form.errors.integerExpected')).min(min, minKey), z.nan()]);

const rankingSchema = z.object({
  points: optionalInt(0, key('players.form.errors.rankingPointsMin')),
  position: optionalInt(1, key('players.form.errors.rankingPositionMin')),
});

export const playerFormSchema = z.object({
  lastName: z
    .string()
    .trim()
    .min(1, key('players.form.errors.lastNameRequired'))
    .max(100, key('players.form.errors.tooLong')),
  firstName: z
    .string()
    .trim()
    .min(1, key('players.form.errors.firstNameRequired'))
    .max(100, key('players.form.errors.tooLong')),
  gender: z.enum(GENDERS, key('players.form.errors.genderRequired')),
  birthDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, key('players.form.errors.birthDateRequired'))
    .refine((value) => value >= '1900-01-01' && value <= today(), {
      message: key('players.form.errors.birthDateInvalid'),
    }),
  clubId: z.uuid(key('players.form.errors.clubRequired')),
  licenseNumber: z.string().trim().max(30, key('players.form.errors.tooLong')),
  email: z.union([z.literal(''), z.email(key('players.form.errors.emailInvalid'))]),
  phone: z.string().trim().max(30, key('players.form.errors.tooLong')),
  rankings: z.object({
    singles: rankingSchema,
    doubles: rankingSchema,
    mixed: rankingSchema,
  }),
});

export type PlayerFormValues = z.infer<typeof playerFormSchema>;

/** Corps envoyé à l'API (création et modification) */
export type PlayerInput = {
  firstName: string;
  lastName: string;
  gender: Gender;
  birthDate: string; // YYYY-MM-DD
  clubId: string;
  licenseNumber: string | null;
  email: string | null;
  phone: string | null;
  rankings: PlayerRankings;
};

type RankingFormValues = PlayerFormValues['rankings']['singles'];

const textOrNull = (value: string) => (value.trim() === '' ? null : value.trim());
const numberOrNull = (value: number) => (Number.isNaN(value) ? null : value);

const toRanking = (ranking: RankingFormValues): Ranking => ({
  points: numberOrNull(ranking.points),
  position: numberOrNull(ranking.position),
});

const toRankingFormValues = (ranking: Ranking): RankingFormValues => ({
  points: ranking.points ?? Number.NaN,
  position: ranking.position ?? Number.NaN,
});

export const emptyRankingFormValues = (): PlayerFormValues['rankings'] => ({
  singles: { points: Number.NaN, position: Number.NaN },
  doubles: { points: Number.NaN, position: Number.NaN },
  mixed: { points: Number.NaN, position: Number.NaN },
});

export function toPlayerInput(values: PlayerFormValues): PlayerInput {
  return {
    firstName: values.firstName,
    lastName: values.lastName,
    gender: values.gender,
    birthDate: values.birthDate,
    clubId: values.clubId,
    licenseNumber: textOrNull(values.licenseNumber),
    email: textOrNull(values.email),
    phone: textOrNull(values.phone),
    rankings: {
      singles: toRanking(values.rankings.singles),
      doubles: toRanking(values.rankings.doubles),
      mixed: toRanking(values.rankings.mixed),
    },
  };
}

export function toPlayerFormValues(player: Player): PlayerFormValues {
  return {
    firstName: player.firstName,
    lastName: player.lastName,
    gender: player.gender,
    birthDate: player.birthDate,
    clubId: player.club.id,
    licenseNumber: player.licenseNumber ?? '',
    email: player.email ?? '',
    phone: player.phone ?? '',
    rankings: {
      singles: toRankingFormValues(player.rankings.singles),
      doubles: toRankingFormValues(player.rankings.doubles),
      mixed: toRankingFormValues(player.rankings.mixed),
    },
  };
}
