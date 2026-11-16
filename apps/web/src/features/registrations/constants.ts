import type { Discipline, Gender, RegistrationStatus } from '@/api/models';

export const REGISTRATION_STATUSES = [
  'CONFIRMED',
  'PENDING_PARTNER',
  'LOOKING_FOR_PARTNER',
] as const satisfies readonly RegistrationStatus[];

const DOUBLES: readonly Discipline[] = ['MD', 'WD', 'XD', 'OD'];

export const isDoubles = (discipline: Discipline) => DOUBLES.includes(discipline);

export function requiredGender(discipline: Discipline, partnerOf?: Gender): Gender | null {
  if (discipline === 'MS' || discipline === 'MD') return 'MALE';
  if (discipline === 'WS' || discipline === 'WD') return 'FEMALE';
  if (discipline === 'XD' && partnerOf) return partnerOf === 'MALE' ? 'FEMALE' : 'MALE';
  return null;
}

export const PICKER_PAGE_SIZE = 8;
