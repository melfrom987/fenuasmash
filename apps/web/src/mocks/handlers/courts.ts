import { delay, http, HttpResponse } from 'msw';
import type { Court } from '@/api/models';
import { db } from '../data';

const courtsOf = (tournamentId: string) =>
  db.courts.filter((c) => c.tournamentId === tournamentId).sort((a, b) => a.number - b.number);

export const courtHandlers = [
  http.get('/api/tournaments/:tournamentId/courts', async ({ params }) => {
    await delay();
    return HttpResponse.json(courtsOf(String(params.tournamentId)));
  }),

  // Ajoute un terrain avec le numéro suivant
  http.post('/api/tournaments/:tournamentId/courts', async ({ params }) => {
    await delay();
    const tournamentId = String(params.tournamentId);
    if (!db.tournaments.some((t) => t.id === tournamentId)) {
      return HttpResponse.json(
        { code: 'TOURNAMENT_NOT_FOUND', message: 'Tournament not found' },
        { status: 404 },
      );
    }

    const number = courtsOf(tournamentId).length + 1;
    const court: Court = { id: crypto.randomUUID(), tournamentId, number, active: true };
    db.courts.push(court);
    return HttpResponse.json(court, { status: 201 });
  }),

  // Active / désactive un terrain
  http.patch('/api/courts/:courtId', async ({ params, request }) => {
    await delay();
    const court = db.courts.find((c) => c.id === params.courtId);
    if (!court) {
      return HttpResponse.json(
        { code: 'COURT_NOT_FOUND', message: 'Court not found' },
        { status: 404 },
      );
    }
    const { active } = (await request.json()) as { active: boolean };
    court.active = active;
    return HttpResponse.json(court);
  }),

  // Retire un terrain : seulement le dernier (et, côté back, seulement s'il n'a jamais servi)
  http.delete('/api/courts/:courtId', async ({ params }) => {
    await delay();
    const court = db.courts.find((c) => c.id === params.courtId);
    if (!court) {
      return HttpResponse.json(
        { code: 'COURT_NOT_FOUND', message: 'Court not found' },
        { status: 404 },
      );
    }
    if (court.number !== courtsOf(court.tournamentId).length) {
      return HttpResponse.json(
        { code: 'COURT_NOT_LAST', message: 'Only the last court can be removed' },
        { status: 409 },
      );
    }
    db.courts.splice(db.courts.indexOf(court), 1);
    return new HttpResponse(null, { status: 204 });
  }),
];
