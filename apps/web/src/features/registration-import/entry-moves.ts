import type { ImportRow } from './types';

export type EntryMove = { rowNumber: number; eventId: string; targetEventId: string };
export type EntryMoves = Record<string, string>;

const moveKey = (rowNumber: number, eventId: string) => `${rowNumber}:${eventId}`;

export function applyMoves(rows: ImportRow[], moves: EntryMoves): ImportRow[] {
  return rows.map((row) => {
    const seen = new Set<string>();
    const entries = [];
    for (const entry of row.entries) {
      const eventId = moves[moveKey(row.rowNumber, entry.eventId)] ?? entry.eventId;
      if (seen.has(eventId)) continue;
      seen.add(eventId);
      entries.push({ ...entry, eventId });
    }
    return { ...row, entries };
  });
}

export function recordMoves(rows: ImportRow[], moves: EntryMoves, list: EntryMove[]): EntryMoves {
  const next = { ...moves };
  for (const move of list) {
    const original =
      rows
        .find((r) => r.rowNumber === move.rowNumber)
        ?.entries.map((e) => e.eventId)
        .find((id) => (next[moveKey(move.rowNumber, id)] ?? id) === move.eventId) ?? move.eventId;
    const key = moveKey(move.rowNumber, original);
    if (move.targetEventId === original) delete next[key];
    else next[key] = move.targetEventId;
  }
  return next;
}
