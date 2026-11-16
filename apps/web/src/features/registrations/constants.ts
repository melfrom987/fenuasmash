import type { Discipline, Gender, RegistrationStatus } from '@/api/models';

export const REGISTRATION_STATUSES = [
  'CONFIRMED',
  'PENDING_PARTNER',
  'LOOKING_FOR_PARTNER',
] as const satisfies readonly RegistrationStatus[];

const DOUBLES: readonly Discipline[] = ['MD', 'WD', 'XD', 'OD'];

/** Un tableau de double se joue par paires */
export const isDoubles = (discipline: Discipline) => DOUBLES.includes(discipline);

/**
 * Genre imposé par la discipline (null = libre).
 * En mixte, le partenaire doit être du genre opposé au joueur.
 */
export function requiredGender(discipline: Discipline, partnerOf?: Gender): Gender | null {
  if (discipline === 'MS' || discipline === 'MD') return 'MALE';
  if (discipline === 'WS' || discipline === 'WD') return 'FEMALE';
  if (discipline === 'XD' && partnerOf) return partnerOf === 'MALE' ? 'FEMALE' : 'MALE';
  return null;
}

/** Taille d'une page de résultats dans le sélecteur de joueur */
export const PICKER_PAGE_SIZE = 8;
