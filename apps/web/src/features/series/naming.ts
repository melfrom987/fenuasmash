export { MIN_EVENT_ENTRIES as MIN_SERIES_ENTRIES } from '@/features/draw/algorithm';
export const MAX_SERIES = 8;

export const seriesEventName = (parentName: string, label: string) =>
  `${parentName} ${label.trim()}`;
