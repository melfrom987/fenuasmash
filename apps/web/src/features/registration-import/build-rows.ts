import type { Club, Event } from '@/api/models';
import { isDoubles } from '@/features/registrations/constants';
import { normalizeText } from '@/lib/text';
import { parseDate } from './dates';
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

export type RowProblem = {
  rowNumber: number;
  code: 'NAME_MISSING' | 'DATE_UNREADABLE' | 'GENDER_UNKNOWN' | 'CLUB_MISSING';
  value: string;
};

export type NameOverrides = Record<number, { firstName: string; lastName: string }>;

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
  nameOverrides,
}: {
  table: Table;
  mapping: ColumnMapping;
  values: ValueMapping;
  events: Event[];
  clubs: Club[];
  nameOverrides: NameOverrides;
}): { rows: ImportRow[]; problems: RowProblem[] } {
  const { fields } = mapping;
  const knownClubs = new Set(clubs.map((c) => normalizeText(c.name)));
  const rows: ImportRow[] = [];
  const problems: RowProblem[] = [];

  table.rows.forEach((cells, index) => {
    const rowNumber = index + 2;
    const text = (column: number | null) => (column === null ? '' : cellText(cells[column]));
    const fail = (code: RowProblem['code'], value: string) =>
      problems.push({ rowNumber, code, value });

    const fullName = text(fields.fullName);
    const name = nameOverrides[rowNumber] ?? splitFullName(fullName, mapping.nameOrder);
    if (!name.lastName || !name.firstName) return fail('NAME_MISSING', fullName);

    const birthCell = fields.birthDate === null ? '' : (cells[fields.birthDate] ?? '');
    const birthDate = parseDate(birthCell, mapping.dateOrder);
    if (!birthDate) return fail('DATE_UNREADABLE', cellText(birthCell));

    const genderValue = text(fields.gender);
    const gender = values.genders[genderValue];
    if (!gender) return fail('GENDER_UNKNOWN', genderValue);

    const clubName = text(fields.club);
    if (!clubName) return fail('CLUB_MISSING', '');
    const clubKey = normalizeText(clubName);

    const entries: ImportEntry[] = [];
    for (const { column, partnerColumn } of mapping.events) {
      const eventId = values.events[column]?.[text(column)];
      const event = events.find((e) => e.id === eventId);
      if (!event || eventId === IGNORE || entries.some((e) => e.eventId === event.id)) continue;
      entries.push({ eventId: event.id, partner: partnerOf(event, text(partnerColumn)) });
    }

    rows.push({
      rowNumber,
      firstName: name.firstName,
      lastName: name.lastName,
      birthDate,
      gender,
      clubName,
      clubCity: knownClubs.has(clubKey) ? null : (values.clubCities[clubKey]?.trim() ?? null),
      email: text(fields.email) || null,
      phone: text(fields.phone) || null,
      entries,
    });
  });

  return { rows, problems };
}
