import type { MatchStatus } from '@/api/models';

export const MATCH_STATUSES: MatchStatus[] = [
  'SCHEDULED',
  'READY',
  'CALLED',
  'WARMUP',
  'IN_PROGRESS',
  'FINISHED',
  'WALKOVER',
  'CANCELLED',
];

export const TRANSITIONS: Record<MatchStatus, MatchStatus[]> = {
  SCHEDULED: ['READY', 'CANCELLED', 'WALKOVER'],
  READY: ['SCHEDULED', 'CALLED', 'CANCELLED', 'WALKOVER'],
  CALLED: ['READY', 'WARMUP', 'CANCELLED', 'WALKOVER'],
  WARMUP: ['CALLED', 'IN_PROGRESS', 'CANCELLED', 'WALKOVER'],
  IN_PROGRESS: ['FINISHED'],
  FINISHED: ['IN_PROGRESS'],
  CANCELLED: [],
  WALKOVER: [],
};

export const ON_COURT: MatchStatus[] = ['CALLED', 'WARMUP', 'IN_PROGRESS'];
export const NEEDS_BOTH_SIDES = (status: MatchStatus) =>
  status !== 'SCHEDULED' && status !== 'CANCELLED';

export const canMove = (from: MatchStatus, to: MatchStatus) => TRANSITIONS[from].includes(to);

export const LIVE_REFRESH_MS = 5000;

export const STATUS_CLASSES: Record<MatchStatus, string> = {
  SCHEDULED: 'bg-sable-100 text-sable-700',
  READY: 'bg-ocean-100 text-ocean-800',
  CALLED: 'bg-sunset-100 text-sunset-800',
  WARMUP: 'bg-sunset-100 text-sunset-800',
  IN_PROGRESS: 'bg-lagon-100 text-lagon-800',
  FINISHED: 'bg-sable-200 text-sable-800',
  WALKOVER: 'bg-hibiscus-100 text-hibiscus-800',
  CANCELLED: 'bg-sable-100 text-sable-500 line-through',
};
