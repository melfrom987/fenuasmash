import { delay, http, HttpResponse } from 'msw';
import type { PairingCode, Tablet } from '@/api/models';
import { db } from '../data';
import { problem } from './utils';

const CODE_ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
const CODE_LIFETIME_MS = 10 * 60 * 1000;
const PINGING_TABLET_IDS = new Set(['30000000-0000-4000-8000-000000000001']);

const codes = new Map<string, PairingCode>();

const newCode = () =>
  Array.from(
    { length: 6 },
    () => CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)],
  ).join('');

const normalizeCode = (value: string) => value.replace(/[\s-]/g, '').toUpperCase();

const byCourtThenRecent = (a: Tablet, b: Tablet) =>
  a.courtNumber - b.courtNumber || b.pairedAt.localeCompare(a.pairedAt);

export const tabletHandlers = [
  http.post('/api/courts/:courtId/pairing-code', async ({ params }) => {
    await delay();
    const court = db.courts.find((c) => c.id === params.courtId);
    if (!court) return problem(404, 'COURT_NOT_FOUND', 'Court not found');
    if (!court.active) return problem(409, 'COURT_INACTIVE', 'The court is disabled');

    for (const [code, pairing] of codes) if (pairing.courtId === court.id) codes.delete(code);
    const pairing: PairingCode = {
      code: newCode(),
      courtId: court.id,
      expiresAt: new Date(Date.now() + CODE_LIFETIME_MS).toISOString(),
    };
    codes.set(pairing.code, pairing);
    return HttpResponse.json(pairing, { status: 201 });
  }),

  http.post('/api/tablets/pair', async ({ request }) => {
    await delay();
    const { code = '', deviceName = null } = (await request.json()) as {
      code?: string;
      deviceName?: string | null;
    };
    const pairing = codes.get(normalizeCode(code));
    if (!pairing || Date.parse(pairing.expiresAt) < Date.now()) {
      return problem(409, 'INVALID_PAIRING_CODE', 'Wrong, expired or used code');
    }
    codes.delete(pairing.code);
    const court = db.courts.find((c) => c.id === pairing.courtId)!;
    const now = new Date().toISOString();
    for (const tablet of db.tablets) {
      if (tablet.courtId === court.id && tablet.revokedAt === null) tablet.revokedAt = now;
    }
    const tablet: Tablet = {
      id: crypto.randomUUID(),
      courtId: court.id,
      courtNumber: court.number,
      deviceName,
      pairedAt: now,
      lastSeenAt: now,
      revokedAt: null,
    };
    db.tablets.push(tablet);
    PINGING_TABLET_IDS.add(tablet.id);
    return HttpResponse.json({
      tabletId: tablet.id,
      accessToken: `mock-tablet.${tablet.id}`,
      tokenType: 'Bearer',
      court,
    });
  }),

  http.get('/api/tournaments/:tournamentId/tablets', async ({ params }) => {
    await delay();
    const courtIds = new Set(
      db.courts.filter((c) => c.tournamentId === params.tournamentId).map((c) => c.id),
    );
    const now = new Date().toISOString();
    for (const tablet of db.tablets) {
      if (PINGING_TABLET_IDS.has(tablet.id) && tablet.revokedAt === null) tablet.lastSeenAt = now;
    }
    return HttpResponse.json(
      db.tablets.filter((t) => courtIds.has(t.courtId)).sort(byCourtThenRecent),
    );
  }),

  http.post('/api/tablets/:tabletId/revoke', async ({ params }) => {
    await delay();
    const tablet = db.tablets.find((t) => t.id === params.tabletId);
    if (!tablet) return problem(404, 'TABLET_NOT_FOUND', 'Tablet not found');
    tablet.revokedAt ??= new Date().toISOString();
    return HttpResponse.json(tablet);
  }),
];
