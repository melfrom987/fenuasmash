import Papa from 'papaparse';
import type { Cell, Table } from './types';

export class UnreadableFileError extends Error {}

const isExcel = (file: File) => /\.xlsx$/i.test(file.name);

function toTable(fileName: string, raw: Cell[][]): Table {
  const rows = raw
    .map((row) => row.map((cell) => (typeof cell === 'string' ? cell.trim() : cell)))
    .filter((row) => row.some((cell) => cell instanceof Date || cell !== ''));
  const [headers = [], ...data] = rows;
  if (headers.length === 0 || data.length === 0) throw new UnreadableFileError('Empty file');
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
  try {
    return toTable(file.name, isExcel(file) ? await readXlsx(file) : await readCsv(file));
  } catch (error) {
    if (error instanceof UnreadableFileError) throw error;
    throw new UnreadableFileError(error instanceof Error ? error.message : String(error));
  }
}
