import { z } from 'zod';
import type { Club } from '@/api/models';
import { key } from '@/i18n';

export const clubFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, key('clubs.form.errors.nameRequired'))
    .max(100, key('clubs.form.errors.tooLong')),
  city: z
    .string()
    .trim()
    .min(1, key('clubs.form.errors.cityRequired'))
    .max(100, key('clubs.form.errors.tooLong')),
  address: z.string().trim().max(200, key('clubs.form.errors.tooLong')),
  email: z.union([z.literal(''), z.email(key('clubs.form.errors.emailInvalid'))]),
  phone: z.string().trim().max(30, key('clubs.form.errors.tooLong')),
});

export type ClubFormValues = z.infer<typeof clubFormSchema>;

export type ClubInput = {
  name: string;
  city: string;
  address: string | null;
  email: string | null;
  phone: string | null;
};

const textOrNull = (value: string) => (value.trim() === '' ? null : value.trim());

export const toClubInput = (values: ClubFormValues): ClubInput => ({
  name: values.name,
  city: values.city,
  address: textOrNull(values.address),
  email: textOrNull(values.email),
  phone: textOrNull(values.phone),
});

export const toClubFormValues = (club: Club): ClubFormValues => ({
  name: club.name,
  city: club.city,
  address: club.address ?? '',
  email: club.email ?? '',
  phone: club.phone ?? '',
});
