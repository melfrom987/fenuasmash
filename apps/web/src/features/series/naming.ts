export const MIN_SERIES_ENTRIES = 2;
export const MAX_SERIES = 8;

export const seriesEventName = (parentName: string, label: string) =>
  `${parentName} ${label.trim()}`;
