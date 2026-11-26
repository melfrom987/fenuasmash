export type EventColor = { strong: string; soft: string; text: string };

export const EVENT_COLORS: EventColor[] = [
  { strong: '#0e9f9a', soft: '#e3f6f5', text: '#0b5f5c' },
  { strong: '#e8742e', soft: '#fdeee3', text: '#8a3f12' },
  { strong: '#7c5cd6', soft: '#efeafb', text: '#45318a' },
  { strong: '#d6457a', soft: '#fbe6ee', text: '#86224a' },
  { strong: '#2f7fd1', soft: '#e5f0fb', text: '#1b4c80' },
  { strong: '#5f9b2e', soft: '#ecf5e3', text: '#365a18' },
  { strong: '#c9a227', soft: '#faf3dc', text: '#6e5710' },
  { strong: '#8a6a4f', soft: '#f2ebe5', text: '#523d2c' },
  { strong: '#d0423c', soft: '#fbe5e4', text: '#7f211d' },
  { strong: '#3a9fbf', soft: '#e4f3f8', text: '#1d5a6d' },
];

export function eventColors(eventIds: string[]) {
  return new Map(eventIds.map((id, index) => [id, EVENT_COLORS[index % EVENT_COLORS.length]]));
}
