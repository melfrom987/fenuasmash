import type { Tablet } from '@/api/models';

export const ONLINE_DELAY_MS = 2 * 60 * 1000;
export const POLL_MS = 15_000;
export const POLL_WHILE_PAIRING_MS = 3_000;

export const isOnline = (tablet: Tablet, now = Date.now()) =>
  now - new Date(tablet.lastSeenAt).getTime() < ONLINE_DELAY_MS;

export const formatCode = (code: string) => `${code.slice(0, 3)} ${code.slice(3)}`;

export function pairedTabletOf(tablets: Tablet[], courtId: string) {
  return tablets.find((tablet) => tablet.courtId === courtId && tablet.revokedAt === null);
}
