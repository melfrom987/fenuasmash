import { delay, http, HttpResponse } from 'msw';

const tournaments = [
  {
    id: '6f1c2a3e-0001-4a7b-9c1d-000000000001',
    name: 'Open de Papeete',
    startsAt: '2026-11-14T18:00:00Z',
    endsAt: '2026-11-16T04:00:00Z',
  },
];

export const tournamentHandlers = [
  http.get('/api/tournaments', async () => {
    await delay();
    return HttpResponse.json(tournaments);
  }),
];
