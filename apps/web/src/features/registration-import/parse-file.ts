import Papa from 'papaparse';
import type { Cell, Table } from './types';

export type UnreadableReason = 'EMPTY' | 'NO_ROWS' | 'UNSUPPORTED' | 'UNREADABLE';

export class UnreadableFileError extends Error {
  reason: UnreadableReason;

  constructor(reason: UnreadableReason, message: string) {
    super(message);
    this.reason = reason;
  }
}

const isExcel = (file: File) => /\.xlsx$/i.test(file.name);
const isUnsupported = (file: File) => /\.(xls|ods|numbers)$/i.test(file.name);

function toTable(fileName: string, raw: Cell[][]): Table {
  const rows = raw
    .map((row) => row.map((cell) => (typeof cell === 'string' ? cell.trim() : cell)))
    .filter((row) => row.some((cell) => cell instanceof Date || cell !== ''));
  const [headers = [], ...data] = rows;
  if (headers.length === 0) throw new UnreadableFileError('EMPTY', 'Empty file');
  if (data.length === 0) throw new UnreadableFileError('NO_ROWS', 'Only the header row');
  return { fileName, headers: headers.map((h) => String(h)), rows: data };
}

async function readCsv(file: File): Promise<Cell[][]> {
  const text = await file.text();
  const result = Papa.parse<string[]>(text, { skipEmptyLines: 'greedy' });
  return result.data;
}

async function readXlsx(file: File): Promise<Cell[][]> {
  const { readSheet } = await import('read-excel-file/browser');
  const rows = (await readSheet(file)) as unknown[][];
  return rows.map((row) =>
    row.map((cell) =>
      cell instanceof Date ? cell : cell === null || cell === undefined ? '' : String(cell),
    ),
  );
}

export async function readTable(file: File): Promise<Table> {
  if (isUnsupported(file)) throw new UnreadableFileError('UNSUPPORTED', file.name);
  try {
    return toTable(file.name, isExcel(file) ? await readXlsx(file) : await readCsv(file));
  } catch (error) {
    if (error instanceof UnreadableFileError) throw error;
    console.error('[import] unreadable file', error);
    throw new UnreadableFileError('UNREADABLE', String(error));
  }
}
