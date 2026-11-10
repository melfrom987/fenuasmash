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
      return HttpResponse.json(
        { code: 'TOURNAMENT_NOT_FOUND', detail: 'Tournament not found' },
        { status: 404 },
      );
    }
    return HttpResponse.json(tournament);
  }),

  http.patch('/api/tournaments/:tournamentId', async ({ params, request }) => {
    await delay();
    const tournament = db.tournaments.find((t) => t.id === params.tournamentId);
    if (!tournament) {
      return HttpResponse.json(
        { code: 'TOURNAMENT_NOT_FOUND', detail: 'Tournament not found' },
        { status: 404 },
      );
    }

    const body = (await request.json()) as Partial<Tournament>;
    const scoring = body.scoring;
    if (
      scoring?.twoPointLead &&
      (scoring.pointCap == null || scoring.pointCap < scoring.pointsToWin)
    ) {
      return HttpResponse.json(
        { code: 'INVALID_SCORING_RULES', detail: 'pointCap must be >= pointsToWin' },
        { status: 400 },
      );
    }

    Object.assign(tournament, body, { updatedAt: new Date().toISOString() });
    return HttpResponse.json(tournament);
  }),

  http.post('/api/tournaments', async ({ request }) => {
    await delay();
    const { courtCount, ...body } = (await request.json()) as Omit<
      Tournament,
      'id' | 'createdAt' | 'updatedAt'
    > & { courtCount: number };

    if (!db.organizations.some((o) => o.id === body.organizationId)) {
      return HttpResponse.json(
        { code: 'INVALID_ORGANIZATION', detail: 'Organization not found' },
        { status: 400 },
      );
    }

    const now = new Date().toISOString();
    const tournament: Tournament = {
      ...body,
      id: crypto.randomUUID(),
      createdAt: now,
      updatedAt: now,
    };
    db.tournaments.push(tournament);

    // Terrains numérotés de 1 à N, créés avec le tournoi
    for (let number = 1; number <= courtCount; number++) {
      db.courts.push({
        id: crypto.randomUUID(),
        tournamentId: tournament.id,
        number,
        active: true,
      });
    }

    return HttpResponse.json(tournament, { status: 201 });
  }),
];
