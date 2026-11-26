import type { Court, Match } from '@/api/models';

const TIME_ZONE = 'Pacific/Tahiti';

const dayKeyFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

export const dayKey = (iso: string) => dayKeyFormatter.format(new Date(iso));

export type ScheduleRow = { start: string; cells: (Match | null)[] };
export type ScheduleDay = { key: string; firstStart: string; rows: ScheduleRow[] };

export function buildGrid(matches: Match[], courts: Court[]): ScheduleDay[] {
  const planned = matches.filter((m) => m.plannedStartAt && m.plannedCourtId);
  const starts = [...new Set(planned.map((m) => m.plannedStartAt as string))].sort();
  const days = new Map<string, ScheduleDay>();

  for (const start of starts) {
    const key = dayKey(start);
    const day = days.get(key) ?? { key, firstStart: start, rows: [] };
    day.rows.push({
      start,
      cells: courts.map(
        (court) =>
          planned.find((m) => m.plannedStartAt === start && m.plannedCourtId === court.id) ?? null,
      ),
    });
    days.set(key, day);
  }
  return [...days.values()];
}

export function gridCourts(matches: Match[], courts: Court[]) {
  const used = new Set(matches.map((m) => m.plannedCourtId));
  return courts.filter((c) => c.active || used.has(c.id)).sort((a, b) => a.number - b.number);
}
