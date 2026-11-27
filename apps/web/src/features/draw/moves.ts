import type { Event, Registration } from '@/api/models';
import { eventsFor } from '@/features/registrations/category-switch';

export function commonTargets(registrations: Registration[], event: Event, events: Event[]) {
  if (registrations.length === 0) return [];
  const [first, ...rest] = registrations.map((r) => eventsFor(r, event, events));
  return first.filter((target) => rest.every((list) => list.some((e) => e.id === target.id)));
}

export function eventsToBringIn(
  event: Event,
  events: Event[],
  registrations: Registration[],
): { event: Event; registrations: Registration[] }[] {
  return events
    .filter((source) => source.id !== event.id)
    .map((source) => ({
      event: source,
      registrations: registrations.filter((r) => r.eventId === source.id),
    }))
    .filter(
      ({ event: source, registrations: entries }) =>
        entries.length > 0 &&
        entries.every((r) => eventsFor(r, source, events).some((e) => e.id === event.id)),
    );
}
