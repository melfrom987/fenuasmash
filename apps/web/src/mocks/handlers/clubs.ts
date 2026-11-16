import { delay, http, HttpResponse } from 'msw';
import type { Club } from '@/api/models';
import type { ClubInput } from '@/features/clubs/schema';
import { LOGO_MAX_BYTES, LOGO_TYPES } from '@/features/clubs/logo';
import { db } from '../data';
import { normalize, problem } from './utils';

const byName = (a: Club, b: Club) => a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' });

const nameTaken = (name: string, exceptId?: string) =>
  db.clubs.some((c) => c.id !== exceptId && normalize(c.name) === normalize(name));

const blank = (fields: string[]) =>
  problem(
    400,
    'VALIDATION_FAILED',
    'Invalid request',
    fields.map((field) => ({ field, message: 'must not be blank' })),
  );

const textOrNull = (value: string | null | undefined) => value?.trim() || null;

const syncPlayers = (club: Club) => {
  for (const player of db.players) {
    if (player.club.id === club.id) {
      player.club = { id: club.id, name: club.name, logoUrl: club.logoUrl };
    }
  }
};

const findClub = (clubId: unknown) => db.clubs.find((c) => c.id === clubId);
const clubNotFound = () => problem(404, 'CLUB_NOT_FOUND', 'Club not found');

export const clubHandlers = [
  http.get('/api/clubs', async ({ request }) => {
    await delay();
    const q = normalize(new URL(request.url).searchParams.get('q') ?? '');
    return HttpResponse.json(
      db.clubs.filter((c) => !q || normalize(c.name).includes(q)).sort(byName),
    );
  }),

  http.post('/api/clubs', async ({ request }) => {
    await delay();
    const body = (await request.json()) as Partial<ClubInput>;
    const name = (body.name ?? '').trim();
    const city = (body.city ?? '').trim();
    const missing = [!name && 'name', !city && 'city'].filter((f): f is string => !!f);
    if (missing.length > 0) return blank(missing);
    if (nameTaken(name)) {
      return problem(409, 'CLUB_NAME_ALREADY_EXISTS', 'A club with this name already exists');
    }
    const club: Club = {
      id: crypto.randomUUID(),
      name,
      city,
      address: textOrNull(body.address),
      email: textOrNull(body.email),
      phone: textOrNull(body.phone),
      logoUrl: null,
    };
    db.clubs.push(club);
    return HttpResponse.json(club, { status: 201 });
  }),

  http.get('/api/clubs/:clubId', async ({ params }) => {
    await delay();
    const club = findClub(params.clubId);
    return club ? HttpResponse.json(club) : clubNotFound();
  }),

  http.patch('/api/clubs/:clubId', async ({ params, request }) => {
    await delay();
    const club = findClub(params.clubId);
    if (!club) return clubNotFound();

    const body = (await request.json()) as Partial<ClubInput>;
    if (body.name !== undefined) {
      const name = body.name.trim();
      if (!name) return blank(['name']);
      if (nameTaken(name, club.id)) {
        return problem(409, 'CLUB_NAME_ALREADY_EXISTS', 'A club with this name already exists');
      }
      club.name = name;
    }
    if (body.city !== undefined) {
      if (!body.city.trim()) return blank(['city']);
      club.city = body.city.trim();
    }
    if (body.address !== undefined) club.address = textOrNull(body.address);
    if (body.email !== undefined) club.email = textOrNull(body.email);
    if (body.phone !== undefined) club.phone = textOrNull(body.phone);
    syncPlayers(club);
    return HttpResponse.json(club);
  }),

  http.put('/api/clubs/:clubId/logo', async ({ params, request }) => {
    await delay();
    const club = findClub(params.clubId);
    if (!club) return clubNotFound();

    const file = (await request.formData()).get('file');
    if (!(file instanceof File) || !LOGO_TYPES.includes(file.type)) {
      return problem(415, 'UNSUPPORTED_LOGO_TYPE', 'Logo must be a PNG, JPEG or WebP image');
    }
    if (file.size > LOGO_MAX_BYTES) {
      return problem(413, 'LOGO_TOO_LARGE', 'Logo must not exceed 1 MB');
    }
    club.logoUrl = URL.createObjectURL(file);
    syncPlayers(club);
    return HttpResponse.json(club);
  }),

  http.delete('/api/clubs/:clubId/logo', async ({ params }) => {
    await delay();
    const club = findClub(params.clubId);
    if (!club) return clubNotFound();
    club.logoUrl = null;
    syncPlayers(club);
    return new HttpResponse(null, { status: 204 });
  }),
];
