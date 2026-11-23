import type { Club, Event, Gender } from '@/api/models';
import { normalizeText } from '@/lib/text';
import { detectDateOrder } from './dates';
import { disciplineFamily, fitsFamily, fitsGender, valueGender } from './event-guess';
import {
  FIELDS,
  IGNORE,
  type Cell,
  type ColumnMapping,
  type EventColumn,
  type Field,
  type Table,
  type ValueMapping,
} from './types';

export const cellText = (cell: Cell | undefined) =>
  cell instanceof Date ? cell.toISOString().slice(0, 10) : (cell ?? '').trim();

const FIELD_HINTS: Record<Field, RegExp> = {
  registeredAt: /horodat|timestamp|date d.?inscription|submitted|envoy/,
  club: /\bclub\b/,
  email: /mail/,
  phone: /\btel\b|telephone|phone|portable|mobile/,
  birthDate: /naissance|birth/,
  gender: /sexe|genre|gender/,
  fullName: /\bnom\b|name/,
};
const PARTNER_HINT = /partenaire|partner/;
const EVENT_HINT = /tableau|simple|double|mixte|single|mixed|epreuve/;

export function suggestColumnMapping(table: Table): ColumnMapping {
  const headers = table.headers.map(normalizeText);
  const used = new Set<number>();
  const fields = Object.fromEntries(FIELDS.map((f) => [f, null])) as Record<Field, number | null>;

  for (const field of Object.keys(FIELD_HINTS) as Field[]) {
    const index = headers.findIndex(
      (h, i) => !used.has(i) && !PARTNER_HINT.test(h) && FIELD_HINTS[field].test(h),
    );
    if (index !== -1) {
      fields[field] = index;
      used.add(index);
    }
  }

  const events: EventColumn[] = [];
  headers.forEach((header, index) => {
    if (used.has(index) || PARTNER_HINT.test(header) || !EVENT_HINT.test(header)) return;
    const next = headers[index + 1];
    const partnerColumn = next !== undefined && PARTNER_HINT.test(next) ? index + 1 : null;
    events.push({ column: index, partnerColumn });
  });

  const birthColumn = fields.birthDate;
  const dateOrder =
    birthColumn === null
      ? 'DMY'
      : detectDateOrder(table.rows.map((row) => row[birthColumn] ?? '')).order;

  return { fields, events, nameOrder: 'LAST_FIRST', dateOrder };
}

export function distinctValues(table: Table, column: number) {
  const counts = new Map<string, number>();
  for (const row of table.rows) {
    const value = cellText(row[column]);
    if (value) counts.set(value, (counts.get(value) ?? 0) + 1);
  }
  return [...counts.entries()]
    .map(([value, count]) => ({ value, count }))
    .sort((a, b) => b.count - a.count || a.value.localeCompare(b.value));
}

const NO_VALUES = new Set(['non', 'no', 'aucun', 'aucune', '-']);
const YES_VALUES = new Set(['oui', 'yes', 'x']);

const tokens = (text: string) =>
  new Set(
    normalizeText(text)
      .split(/[^a-z0-9]+/)
      .filter((t) => t.length > 1),
  );

function bestEvent(text: string, header: string, events: Event[]): string | undefined {
  const gender = valueGender(text);
  const family = disciplineFamily(text) ?? disciplineFamily(header);
  const wanted = tokens(text);
  const scored = events
    .filter((event) => fitsGender(event.discipline, gender) && fitsFamily(event.discipline, family))
    .map((event) => {
      const words = tokens(`${event.name} ${event.categoryLabel ?? ''}`);
      return { id: event.id, score: [...wanted].filter((w) => words.has(w)).length };
    })
    .filter((e) => e.score > 0)
    .sort((a, b) => b.score - a.score);
  if (scored.length === 0) return undefined;
  if (scored.length > 1 && scored[1].score === scored[0].score) return undefined;
  return scored[0].id;
}

export const isYesValue = (value: string) => YES_VALUES.has(normalizeText(value));

export function suggestGender(value: string): Gender | undefined {
  const v = normalizeText(value);
  if (/^(f|w|fille)/.test(v)) return 'FEMALE';
  if (/^(h|m|g)/.test(v)) return 'MALE';
  return undefined;
}

export function suggestValueMapping(
  table: Table,
  mapping: ColumnMapping,
  events: Event[],
): ValueMapping {
  const eventValues: ValueMapping['events'] = {};
  for (const { column } of mapping.events) {
    eventValues[column] = {};
    for (const { value } of distinctValues(table, column)) {
      const v = normalizeText(value);
      const guess = NO_VALUES.has(v)
        ? IGNORE
        : bestEvent(
            YES_VALUES.has(v) ? table.headers[column] : value,
            table.headers[column],
            events,
          );
      if (guess) eventValues[column][value] = guess;
    }
  }

  const genders: ValueMapping['genders'] = {};
  if (mapping.fields.gender !== null) {
    for (const { value } of distinctValues(table, mapping.fields.gender)) {
      const guess = suggestGender(value);
      if (guess) genders[value] = guess;
    }
  }

  return { events: eventValues, genders, clubCities: {} };
}

export function newClubNames(table: Table, mapping: ColumnMapping, clubs: Club[]) {
  const column = mapping.fields.club;
  if (column === null) return [];
  const known = new Set(clubs.map((c) => normalizeText(c.name)));
  const found = new Map<string, string>();
  for (const { value } of distinctValues(table, column)) {
    const key = normalizeText(value);
    if (!known.has(key) && !found.has(key)) found.set(key, value);
  }
  return [...found.entries()].map(([key, name]) => ({ key, name }));
}
