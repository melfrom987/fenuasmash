import type { Cell, DateOrder } from './types';

const pad = (n: number) => String(n).padStart(2, '0');

function toIso(year: number, month: number, day: number): string | null {
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1) return null;
  if (year < 1900 || year > new Date().getFullYear()) return null;
  return `${year}-${pad(month)}-${pad(day)}`;
}

const fullYear = (year: number) => {
  if (year >= 100) return year;
  const currentShort = new Date().getFullYear() % 100;
  return year > currentShort ? 1900 + year : 2000 + year;
};

const fromExcelSerial = (serial: number) => {
  const date = new Date(Date.UTC(1899, 11, 30) + serial * 86_400_000);
  return toIso(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
};

const ISO = /^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/;
const SHORT = /^(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})/;

export function parseDate(cell: Cell, order: DateOrder): string | null {
  if (cell instanceof Date) {
    return toIso(cell.getUTCFullYear(), cell.getUTCMonth() + 1, cell.getUTCDate());
  }
  const value = cell.trim();
  if (/^\d{4,6}(\.\d+)?$/.test(value)) return fromExcelSerial(Math.floor(Number(value)));

  const iso = ISO.exec(value);
  if (iso) return toIso(Number(iso[1]), Number(iso[2]), Number(iso[3]));

  const short = SHORT.exec(value);
  if (short) {
    const [a, b, year] = [Number(short[1]), Number(short[2]), fullYear(Number(short[3]))];
    return order === 'DMY' ? toIso(year, b, a) : toIso(year, a, b);
  }
  return null;
}

export function detectDateOrder(cells: Cell[]): { order: DateOrder; certain: boolean } {
  for (const cell of cells) {
    if (cell instanceof Date) continue;
    const short = SHORT.exec(cell.trim());
    if (!short) continue;
    if (Number(short[1]) > 12) return { order: 'DMY', certain: true };
    if (Number(short[2]) > 12) return { order: 'MDY', certain: true };
  }
  return { order: 'DMY', certain: false };
}

const TIME = /(\d{1,2}):(\d{2})(?::(\d{2}))?/;
const TAHITI_OFFSET_HOURS = 10;

const tahitiToIso = (y: number, mo: number, d: number, h: number, mi: number, s: number) =>
  new Date(Date.UTC(y, mo - 1, d, h + TAHITI_OFFSET_HOURS, mi, s)).toISOString();

export function parseDateTime(cell: Cell, order: DateOrder): string | null {
  if (cell instanceof Date) {
    return tahitiToIso(
      cell.getUTCFullYear(),
      cell.getUTCMonth() + 1,
      cell.getUTCDate(),
      cell.getUTCHours(),
      cell.getUTCMinutes(),
      cell.getUTCSeconds(),
    );
  }
  const value = cell.trim();
  if (/^\d{4,6}\.\d+$/.test(value)) {
    const wall = new Date(Date.UTC(1899, 11, 30) + Number(value) * 86_400_000);
    return parseDateTime(wall, order);
  }
  const date = parseDate(value, order);
  if (!date) return null;
  const time = TIME.exec(value);
  const [y, mo, d] = date.split('-').map(Number);
  return tahitiToIso(
    y,
    mo,
    d,
    time ? Number(time[1]) : 0,
    time ? Number(time[2]) : 0,
    time?.[3] ? Number(time[3]) : 0,
  );
}
