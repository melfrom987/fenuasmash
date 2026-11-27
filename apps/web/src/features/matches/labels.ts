import type { Match, MatchSide, Registration } from '@/api/models';
import { playerName } from '@/features/registrations/format';

export type MatchLabels = {
  entry: (registrationId: string | null) => string | null;
  side: (match: Match, side: MatchSide) => { name: string; known: boolean } | null;
  event: (eventId: string) => string;
  court: (courtId: string | null) => number | null;
  round: (match: Match) => string | null;
};

export const registrationLabel = (registration: Registration) =>
  registration.partner
    ? `${playerName(registration.player)} / ${playerName(registration.partner)}`
    : playerName(registration.player);

export const knockoutStage = (round: number, lastRound: number) => {
  const remaining = lastRound - round;
  if (remaining === 0) return 'final' as const;
  if (remaining === 1) return 'semi' as const;
  if (remaining === 2) return 'quarter' as const;
  return 'roundOf' as const;
};
