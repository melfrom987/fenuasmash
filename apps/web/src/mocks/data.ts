import type { Court, Organization, Tournament } from '@/api/models';

const now = '2026-10-28T00:00:00.000Z';

const organizations: Organization[] = [
  { id: '00000000-0000-4000-8000-000000000001', name: 'Ligue de Badminton de Polynésie', createdAt: now },
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
  },
];

const courts: Court[] = [1, 2, 3].map((n) => ({
  id: `20000000-0000-4000-8000-00000000000${n}`,
  tournamentId: tournaments[0].id,
  label: `Terrain ${n}`,
  active: true,
}));

export const db = { organizations, tournaments, courts };
