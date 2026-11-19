import type { Club, Event, Gender } from '@/api/models';
import { isDoubles } from '@/features/registrations/constants';
import { normalizeText } from '@/lib/text';
import { parseDate, parseDateTime } from './dates';
import { cellText } from './mapping';
import { splitFullName } from './names';
import {
  IGNORE,
  type ColumnMapping,
  type ImportEntry,
  type ImportPartner,
  type ImportRow,
  type Table,
  type ValueMapping,
} from './types';

export type MissingField = 'NAME' | 'BIRTH_DATE' | 'GENDER' | 'CLUB';

export type RowFix = {
  lastName?: string;
  firstName?: string;
  birthDate?: string;
  gender?: Gender;
  clubName?: string;
  playerId?: string;
  skip?: boolean;
};

export type RowFixes = Record<number, RowFix>;

export type RowProblem = {
  rowNumber: number;
  missing: MissingField[];
  skipped: boolean;
  fullName: string;
  rawBirthDate: string;
  draft: {
    lastName: string;
    firstName: string;
    birthDate: string | null;
    gender: Gender | null;
    clubName: string;
  };
  entryCount: number;
};

const LOOKING_HINT = /recherch|looking|cherche/;
const NO_PARTNER = new Set(['', 'non', 'no', '-']);

function partnerOf(event: Event, text: string | null): ImportPartner {
  if (!isDoubles(event.discipline)) return { kind: 'NONE' };
  const value = normalizeText(text ?? '');
  if (NO_PARTNER.has(value) || LOOKING_HINT.test(value)) return { kind: 'LOOKING' };
  return { kind: 'NAMED', name: (text ?? '').trim() };
}

export function buildImportRows({
  table,
  mapping,
  values,
  events,
  clubs,
  fixes,
}: {
  table: Table;
  mapping: ColumnMapping;
  values: ValueMapping;
  events: Event[];
  clubs: Club[];
  fixes: RowFixes;
}): { rows: ImportRow[]; problems: RowProblem[] } {
  const { fields } = mapping;
  const knownClubs = new Set(clubs.map((c) => normalizeText(c.name)));
  const rows: ImportRow[] = [];
  const problems: RowProblem[] = [];

  table.rows.forEach((cells, index) => {
    const rowNumber = index + 2;
    const fix = fixes[rowNumber] ?? {};
    const text = (column: number | null) => (column === null ? '' : cellText(cells[column]));

    const fullName = text(fields.fullName);
    const split = splitFullName(fullName, mapping.nameOrder);
    const lastName = (fix.lastName ?? split.lastName).trim();
    const firstName = (fix.firstName ?? split.firstName).trim();

    const birthCell = fields.birthDate === null ? '' : (cells[fields.birthDate] ?? '');
    const birthDate = fix.birthDate ?? parseDate(birthCell, mapping.dateOrder);
    const gender = fix.gender ?? values.genders[text(fields.gender)] ?? null;
    const clubName = (fix.clubName ?? text(fields.club)).trim();

    const entries: ImportEntry[] = [];
    for (const { column, partnerColumn } of mapping.events) {
      const eventId = values.events[column]?.[text(column)];
      const event = events.find((e) => e.id === eventId);
      if (!event || eventId === IGNORE || entries.some((e) => e.eventId === event.id)) continue;
      entries.push({ eventId: event.id, partner: partnerOf(event, text(partnerColumn)) });
    }

    const missing: MissingField[] = [];
    if (!lastName || !firstName) missing.push('NAME');
    if (!birthDate) missing.push('BIRTH_DATE');
    if (!gender) missing.push('GENDER');
    if (!clubName) missing.push('CLUB');

    if (fix.skip || missing.length > 0 || !birthDate || !gender) {
      problems.push({
        rowNumber,
        missing,
        skipped: fix.skip === true,
        fullName,
        rawBirthDate: cellText(birthCell),
        draft: { lastName, firstName, birthDate, gender, clubName },
        entryCount: entries.length,
      });
      return;
    }

    const clubKey = normalizeText(clubName);
    rows.push({
      rowNumber,
      firstName,
      lastName,
      birthDate,
      gender,
      clubName,
      clubCity: knownClubs.has(clubKey) ? null : (values.clubCities[clubKey]?.trim() ?? null),
      email: text(fields.email) || null,
      phone: text(fields.phone) || null,
      registeredAt:
        fields.registeredAt === null
          ? null
          : parseDateTime(cells[fields.registeredAt] ?? '', mapping.dateOrder),
      playerDecision: fix.playerId ? { action: 'USE_EXISTING', playerId: fix.playerId } : undefined,
      entries,
    });
  });

  return { rows, problems };
}
