import { delay, http, HttpResponse } from 'msw';
import { db } from '../data';
import type { Tournament } from '@/api/models';

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

  http.post('/api/tournaments', async ({ request }) => {
    await delay();
    const body = (await request.json()) as Omit<Tournament, 'id' | 'createdAt' | 'updatedAt'>;

    // Même réponse que le back si l'organisation n'existe pas (erreur Prisma P2003)
    if (!db.organizations.some((o) => o.id === body.organizationId)) {
      return HttpResponse.json({ message: 'Invalid reference' }, { status: 400 });
    }

    const now = new Date().toISOString();
    const tournament: Tournament = {
      ...body,
      id: crypto.randomUUID(),
      createdAt: now,
      updatedAt: now,
    };
    db.tournaments.push(tournament);
    return HttpResponse.json(tournament, { status: 201 });
  }),
];
