import type { Court, Organization, Tournament, Event } from '@/api/models';

const now = '2026-10-28T00:00:00.000Z';

const organizations: Organization[] = [
  {
    id: '00000000-0000-4000-8000-000000000001',
    name: 'Ligue de Badminton de Polynésie',
    createdAt: now,
  },
];

const tournaments: Tournament[] = [
  {
    id: '10000000-0000-4000-8000-000000000001',
    organizationId: organizations[0].id,
    name: 'Open de Papeete',
    startsAt: '2026-11-14T18:00:00.000Z', // 8h à Tahiti
    endsAt: '2026-11-16T04:00:00.000Z',
    minimumRestMinutes: 20,
    createdAt: now,
    updatedAt: now,
    competitionType: 'INDIVIDUAL',
    drawFormat: 'POOLS_THEN_ELIMINATION',
    playMode: 'SETS',
    scoring: { gamesToWin: 2, pointsToWin: 21, twoPointLead: true, pointCap: 30 },
    disciplines: ['MS', 'WS', 'MD', 'WD', 'XD'],
    forbiddenCombinations: [['MS', 'XD']],
  },
  {
    id: '10000000-0000-4000-8000-000000000002',
    organizationId: organizations[0].id,
    name: 'Championnat de Polynésie',
    startsAt: '2026-12-05T18:00:00.000Z',
    endsAt: '2026-12-07T04:00:00.000Z',
    minimumRestMinutes: 20,
    createdAt: now,
    updatedAt: now,
    competitionType: 'INDIVIDUAL',
    drawFormat: 'POOLS_THEN_ELIMINATION',
    playMode: 'SETS',
    scoring: { gamesToWin: 2, pointsToWin: 21, twoPointLead: true, pointCap: 30 },
    disciplines: ['MS', 'WS', 'MD', 'WD', 'XD'],
    forbiddenCombinations: [],
  },
];

const courts: Court[] = [1, 2, 3, 4].map((n) => ({
  id: `20000000-0000-4000-8000-00000000000${n}`,
  tournamentId: tournaments[0].id,
  number: n,
  active: true,
}));

const events: Event[] = [
  {
    id: '30000000-0000-4000-8000-000000000001',
    tournamentId: tournaments[0].id,
    name: 'Simple Homme',
    discipline: 'MS',
    categoryType: 'OPEN',
    categoryLabel: null,
    plannedDate: '2026-11-14T18:00:00.000Z', // samedi 8h à Tahiti
    maxEntries: 24,
    poolCount: null,
    poolSize: null,
    qualifiersPerPool: null,
  },
  {
    id: '30000000-0000-4000-8000-000000000002',
    tournamentId: tournaments[0].id,
    name: 'Simple Homme Cadet',
    discipline: 'MS',
    categoryType: 'AGE',
    categoryLabel: 'Cadet',
    plannedDate: '2026-11-14T18:00:00.000Z',
    maxEntries: null,
    poolCount: null,
    poolSize: null,
    qualifiersPerPool: null,
  },
  {
    id: '30000000-0000-4000-8000-000000000003',
    tournamentId: tournaments[0].id,
    name: 'Double Mixte',
    discipline: 'XD',
    categoryType: 'OPEN',
    categoryLabel: null,
    plannedDate: '2026-11-16T00:00:00.000Z', // dimanche 14h à Tahiti
    maxEntries: 16,
    poolCount: null,
    poolSize: null,
    qualifiersPerPool: null,
  },
];

export const db = { organizations, tournaments, courts, events };
