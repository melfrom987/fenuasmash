import type { Registration } from '@/api/models';
import { playerName } from '@/features/registrations/format';

export type MatchLabels = {
  entry: (registrationId: string | null) => string | null;
  event: (eventId: string) => string;
  court: (courtId: string | null) => number | null;
};

export const registrationLabel = (registration: Registration) =>
  registration.partner
    ? `${playerName(registration.player)} / ${playerName(registration.partner)}`
    : playerName(registration.player);
