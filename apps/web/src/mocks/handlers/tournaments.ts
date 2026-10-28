import { delay, http, HttpResponse } from 'msw';
import { db } from '../data';

export const tournamentHandlers = [
  http.get('/api/tournaments', async () => {
    await delay();
    const sorted = [...db.tournaments].sort((a, b) => b.startsAt.localeCompare(a.startsAt));
    return HttpResponse.json(sorted);
  }),

  http.get('/api/tournaments/:tournamentId', async ({ params }) => {
    await delay();
    const tournament = db.tournaments.find((t) => t.id === params.tournamentId);
    if (!tournament) {
      return HttpResponse.json({ message: 'Tournament not found' }, { status: 404 });
    }
    return HttpResponse.json(tournament);
  }),
];
