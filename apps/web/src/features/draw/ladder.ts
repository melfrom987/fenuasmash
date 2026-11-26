import type { Event } from '@/api/models';
import { DISCIPLINES } from '@/features/events/constants';

const TYPE_ORDER = { OPEN: 0, LEVEL: 1, AGE: 2 } as const;

const byLabel = (a: Event, b: Event) =>
  (a.categoryLabel ?? a.name).localeCompare(b.categoryLabel ?? b.name, undefined, {
    numeric: true,
  });

const byStrength = (a: Event, b: Event) =>
  TYPE_ORDER[a.categoryType] - TYPE_ORDER[b.categoryType] ||
  (a.categoryType === 'AGE'
    ? (a.minBirthYear ?? Number.NEGATIVE_INFINITY) - (b.minBirthYear ?? Number.NEGATIVE_INFINITY)
    : 0) ||
  byLabel(a, b);

export function ladderOf(event: Event, events: Event[]) {
  if (event.parentEventId) {
    return events.filter((e) => e.parentEventId === event.parentEventId).sort(byLabel);
  }
  return events
    .filter((e) => e.discipline === event.discipline && e.parentEventId === null)
    .sort(byStrength);
}

export function neighbours(event: Event, events: Event[]) {
  const ladder = ladderOf(event, events);
  const index = ladder.findIndex((e) => e.id === event.id);
  return { up: ladder[index - 1] ?? null, down: ladder[index + 1] ?? null };
}

export function orderedEvents(events: Event[]) {
  const roots = events.filter((e) => e.parentEventId === null);
  return DISCIPLINES.flatMap((discipline) =>
    roots
      .filter((e) => e.discipline === discipline)
      .sort(byStrength)
      .flatMap((root) => [
        root,
        ...events.filter((e) => e.parentEventId === root.id).sort(byLabel),
      ]),
  );
}
