import type { Event, Player, PlayerSummary, Registration, RegistrationStatus } from '@/api/models';

export const toPlayerSummary = (player: Player): PlayerSummary => ({
  id: player.id,
  firstName: player.firstName,
  lastName: player.lastName,
  gender: player.gender,
  birthDate: player.birthDate,
  clubName: player.club.name,
  licenseNumber: player.licenseNumber,
});

/**
 * Quelques inscriptions de départ (index = position dans la liste des joueurs fictifs :
 * pairs = hommes, impairs = femmes). Respecte l'interdiction Simple + Mixte du tournoi.
 */
export function seedRegistrations(events: Event[], players: Player[]): Registration[] {
  const [singles, , mixed] = events;
  let n = 0;
  const make = (
    event: Event,
    player: number,
    partner: number | null,
    status: RegistrationStatus,
  ): Registration => {
    n += 1;
    return {
      id: `60000000-0000-4000-8000-${String(n).padStart(12, '0')}`,
      tournamentId: event.tournamentId,
      eventId: event.id,
      status,
      player: toPlayerSummary(players[player]),
      partner: partner === null ? null : toPlayerSummary(players[partner]),
      createdAt: new Date(Date.UTC(2026, 9, 1 + n)).toISOString(),
    };
  };

  return [
    ...[0, 2, 4, 6, 8, 10].map((i) => make(singles, i, null, 'CONFIRMED')),
    make(mixed, 12, 1, 'CONFIRMED'),
    make(mixed, 14, 3, 'CONFIRMED'),
    make(mixed, 16, 5, 'CONFIRMED'),
    make(mixed, 18, 7, 'PENDING_PARTNER'),
    make(mixed, 20, null, 'LOOKING_FOR_PARTNER'),
    make(mixed, 9, null, 'LOOKING_FOR_PARTNER'),
  ];
}
