import type { ValueMapping } from './types';

export function mergeValueMapping(suggested: ValueMapping, previous: ValueMapping | null) {
  if (!previous) return suggested;
  const events: ValueMapping['events'] = {};
  for (const [column, values] of Object.entries(suggested.events)) {
    events[Number(column)] = { ...values, ...previous.events[Number(column)] };
  }
  return {
    events,
    genders: { ...suggested.genders, ...previous.genders },
    clubCities: { ...suggested.clubCities, ...previous.clubCities },
  };
}
