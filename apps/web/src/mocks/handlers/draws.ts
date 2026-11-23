import { delay, http, HttpResponse } from 'msw';
import type { Draw, DrawSettings, DrawSlotRef } from '@/api/models';
import { DrawSettingsError, makeDraw, toDrawEntries } from '@/features/draw/algorithm';
import { db } from '../data';
import { problem } from './utils';

const findEvent = (id: unknown) => db.events.find((e) => e.id === id);
const findDraw = (eventId: unknown) => db.draws.find((d) => d.eventId === eventId);
const drawNotFound = () => problem(404, 'DRAW_NOT_FOUND', 'No draw for this event');
const published = () => problem(409, 'DRAW_PUBLISHED', 'The draw is published');

export const isDrawPublished = (eventId: string) => findDraw(eventId)?.status === 'PUBLISHED';

function swapSlots(draw: Draw, a: DrawSlotRef, b: DrawSlotRef) {
  if ('pool' in a && 'pool' in b) {
    const poolA = draw.pools.find((p) => p.name === a.pool);
    const poolB = draw.pools.find((p) => p.name === b.pool);
    const entryA = poolA?.entries.at(a.index);
    const entryB = poolB?.entries.at(b.index);
    if (!poolA || !poolB || !entryA || !entryB) return false;
    poolA.entries[a.index] = entryB;
    poolB.entries[b.index] = entryA;
    return true;
  }
  if ('position' in a && 'position' in b && draw.bracket) {
    const slotA = draw.bracket.slots.find((s) => s.position === a.position);
    const slotB = draw.bracket.slots.find((s) => s.position === b.position);
    if (!slotA || !slotB || slotA.bye || slotB.bye) return false;
    [slotA.entry, slotB.entry] = [slotB.entry, slotA.entry];
    [slotA.qualifier, slotB.qualifier] = [slotB.qualifier, slotA.qualifier];
    return true;
  }
  return false;
}

export const drawHandlers = [
  http.get('/api/events/:eventId/draw', async ({ params }) => {
    await delay();
    const draw = findDraw(params.eventId);
    return draw ? HttpResponse.json(draw) : drawNotFound();
  }),

  http.post('/api/events/:eventId/draw', async ({ params, request }) => {
    await delay(400);
    const event = findEvent(params.eventId);
    if (!event) return problem(404, 'EVENT_NOT_FOUND', 'Event not found');
    if (isDrawPublished(event.id)) return published();
    const tournament = db.tournaments.find((t) => t.id === event.tournamentId);
    const settings = (await request.json()) as DrawSettings;

    const registrations = db.registrations.filter((r) => r.eventId === event.id);
    const confirmed = registrations.filter((r) => r.status === 'CONFIRMED');
    try {
      const plan = makeDraw({
        format: tournament?.drawFormat ?? 'POOLS_THEN_ELIMINATION',
        ranked: toDrawEntries(confirmed, event.discipline),
        poolCount: settings.poolCount,
        qualifiersPerPool: settings.qualifiersPerPool,
        seedCount: settings.seedCount,
      });
      const draw: Draw = {
        eventId: event.id,
        status: 'DRAFT',
        seedCount: plan.seedCount,
        pools: plan.pools,
        bracket: plan.bracket,
        excludedRegistrationIds: registrations
          .filter((r) => r.status !== 'CONFIRMED')
          .map((r) => r.id),
        createdAt: new Date().toISOString(),
        publishedAt: null,
      };
      db.draws = db.draws.filter((d) => d.eventId !== event.id);
      db.draws.push(draw);
      event.poolCount = plan.pools.length > 0 ? plan.pools.length : null;
      event.qualifiersPerPool =
        plan.bracket && plan.pools.length > 0 ? plan.pools[0].qualifiers : null;
      return HttpResponse.json(draw, { status: 201 });
    } catch (error) {
      if (error instanceof DrawSettingsError) return problem(400, error.code, error.message);
      throw error;
    }
  }),

  http.post('/api/events/:eventId/draw/swap', async ({ params, request }) => {
    await delay();
    const draw = findDraw(params.eventId);
    if (!draw) return drawNotFound();
    if (draw.status === 'PUBLISHED') return published();
    const { a, b } = (await request.json()) as { a: DrawSlotRef; b: DrawSlotRef };
    if (!swapSlots(draw, a, b))
      return problem(400, 'INVALID_SWAP', 'These two places cannot be swapped');
    return HttpResponse.json(draw);
  }),

  http.post('/api/events/:eventId/draw/publish', async ({ params }) => {
    await delay();
    const draw = findDraw(params.eventId);
    if (!draw) return drawNotFound();
    draw.status = 'PUBLISHED';
    draw.publishedAt = new Date().toISOString();
    return HttpResponse.json(draw);
  }),

  http.delete('/api/events/:eventId/draw', async ({ params }) => {
    await delay();
    const draw = findDraw(params.eventId);
    if (!draw) return drawNotFound();
    if (draw.status === 'PUBLISHED') return published();
    db.draws = db.draws.filter((d) => d !== draw);
    return new HttpResponse(null, { status: 204 });
  }),
];
