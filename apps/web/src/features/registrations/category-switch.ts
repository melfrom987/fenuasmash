import type { Discipline, Event, Gender, Registration } from '@/api/models';
import { birthYearFits } from '@/features/players/age-category';
import { requiredGender } from './constants';

export type EventCandidate = { birthDate: string; gender: Gender };

const FAMILY: Record<Discipline, 'SINGLES' | 'DOUBLES' | 'MIXED'> = {
  MS: 'SINGLES',
  WS: 'SINGLES',
  MD: 'DOUBLES',
  WD: 'DOUBLES',
  OD: 'DOUBLES',
  XD: 'MIXED',
};

const playersOf = (registration: Registration): EventCandidate[] =>
  [registration.player, registration.partner].filter((p) => p !== null);

const canEnter = (event: Event, player: EventCandidate) => {
  const gender = requiredGender(event.discipline);
  return (gender === null || gender === player.gender) && birthYearFits(event, player.birthDate);
};

export const isOutOfCategory = (event: Event, registration: Registration) =>
  playersOf(registration).some((p) => !birthYearFits(event, p.birthDate));

export function possibleEvents(
  players: EventCandidate[],
  current: Event,
  events: Event[],
): Event[] {
  return events.filter(
    (e) =>
      e.id !== current.id &&
      FAMILY[e.discipline] === FAMILY[current.discipline] &&
      players.every((p) => canEnter(e, p)),
  );
}

export function closestAgeEvent(
  players: EventCandidate[],
  current: Event,
  events: Event[],
): Event | undefined {
  return possibleEvents(players, current, events)
    .filter((e) => e.categoryType === 'AGE')
    .sort(
      (a, b) =>
        (b.minBirthYear ?? -Infinity) - (a.minBirthYear ?? -Infinity) ||
        (a.maxBirthYear ?? Infinity) - (b.maxBirthYear ?? Infinity),
    )
    .at(0);
}

export const suggestCategory = (registration: Registration, current: Event, events: Event[]) =>
  closestAgeEvent(playersOf(registration), current, events);

export const eventsFor = (registration: Registration, current: Event, events: Event[]) =>
  possibleEvents(playersOf(registration), current, events);
