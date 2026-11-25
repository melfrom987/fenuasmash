import { delay, http, HttpResponse } from 'msw';
import type { Event, SeriesSplitRequest } from '@/api/models';
import { MAX_SERIES, MIN_SERIES_ENTRIES, seriesEventName } from '@/features/series/naming';
import { db } from '../data';
import { isDrawPublished } from './draws';
import { normalize, problem } from './utils';

const invalid = (detail: string) => problem(400, 'INVALID_SERIES_SPLIT', detail);

export const seriesHandlers = [
  http.post('/api/events/:eventId/series', async ({ params, request }) => {
    await delay(400);
    const source = db.events.find((e) => e.id === params.eventId);
    if (!source) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');
    if (isDrawPublished(source.id)) return problem(409, 'DRAW_PUBLISHED', 'The draw is published');
    if (source.parentEventId !== null) return invalid('A series cannot be split again');

    const { series } = (await request.json()) as SeriesSplitRequest;
    if (!Array.isArray(series) || series.length < 2 || series.length > MAX_SERIES) {
      return invalid(`Between 2 and ${MAX_SERIES} series`);
    }

    const labels = series.map((s) => normalize(s.label ?? ''));
    if (labels.some((l) => l === '') || new Set(labels).size !== labels.length) {
      return invalid('Series labels must be filled and different');
    }

    const confirmed = new Set(
      db.registrations
        .filter((r) => r.eventId === source.id && r.status === 'CONFIRMED')
        .map((r) => r.id),
    );
    const ids = series.flatMap((s) => s.registrationIds);
    if (new Set(ids).size !== ids.length || ids.some((id) => !confirmed.has(id))) {
      return invalid('Each confirmed registration of the event can be in one series only');
    }
    if (series.some((s) => s.registrationIds.length < MIN_SERIES_ENTRIES)) {
      return invalid(`At least ${MIN_SERIES_ENTRIES} entries per series`);
    }

    const names = series.map((s) => seriesEventName(source.name, s.label));
    const taken = db.events.some(
      (e) =>
        e.tournamentId === source.tournamentId &&
        names.some((n) => n.toLowerCase() === e.name.toLowerCase()),
    );
    if (taken) return problem(409, 'EVENT_NAME_ALREADY_EXISTS', 'Event name already exists');

    db.draws = db.draws.filter((d) => d.eventId !== source.id);
    const created: Event[] = series.map((s, i) => ({
      id: crypto.randomUUID(),
      tournamentId: source.tournamentId,
      name: names[i],
      discipline: source.discipline,
      categoryType: 'LEVEL',
      categoryLabel: s.label.trim(),
      plannedDate: source.plannedDate,
      maxEntries: null,
      minBirthYear: source.minBirthYear,
      maxBirthYear: source.maxBirthYear,
      poolCount: null,
      poolSize: null,
      qualifiersPerPool: null,
      parentEventId: source.id,
    }));
    db.events.push(...created);
    series.forEach((s, i) => {
      for (const registration of db.registrations) {
        if (s.registrationIds.includes(registration.id)) registration.eventId = created[i].id;
      }
    });
    return HttpResponse.json({ events: created }, { status: 201 });
  }),
];
