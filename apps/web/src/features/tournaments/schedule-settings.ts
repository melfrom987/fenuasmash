import { z } from 'zod';
import type { PublicVisibility, Tournament } from '@/api/models';
import { key, type TranslationKey } from '@/i18n';

export const PUBLIC_VISIBILITIES = [
  'ON_PUBLISH',
  'AT_START',
] as const satisfies readonly PublicVisibility[];

export const SCHEDULE_DEFAULTS = {
  minimumRestMinutes: 20,
  averageMatchMinutes: 25,
  convocationLeadMinutes: 30,
  publicVisibility: 'ON_PUBLISH',
} as const;

const minutes = (min: number, max: number, rangeKey: TranslationKey) =>
  z
    .number(key('schedule.errors.numberExpected'))
    .int(key('schedule.errors.numberExpected'))
    .min(min, rangeKey)
    .max(max, rangeKey);

export const scheduleSettingsSchema = z.object({
  minimumRestMinutes: minutes(0, 120, key('schedule.errors.restRange')),
  averageMatchMinutes: minutes(10, 120, key('schedule.errors.durationRange')),
  convocationLeadMinutes: minutes(0, 120, key('schedule.errors.convocationRange')),
  publicVisibility: z.enum(PUBLIC_VISIBILITIES),
});

export type ScheduleSettingsValues = z.infer<typeof scheduleSettingsSchema>;

export function toScheduleSettingsValues(tournament: Tournament): ScheduleSettingsValues {
  return {
    minimumRestMinutes: tournament.minimumRestMinutes,
    averageMatchMinutes: tournament.averageMatchMinutes ?? SCHEDULE_DEFAULTS.averageMatchMinutes,
    convocationLeadMinutes:
      tournament.convocationLeadMinutes ?? SCHEDULE_DEFAULTS.convocationLeadMinutes,
    publicVisibility: tournament.publicVisibility ?? SCHEDULE_DEFAULTS.publicVisibility,
  };
}

export const matchesPerHour = (courts: number, averageMatchMinutes: number) =>
  averageMatchMinutes > 0 ? Math.floor((courts * 60) / averageMatchMinutes) : 0;
