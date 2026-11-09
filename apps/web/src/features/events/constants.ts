import type { EventFormat, EventType, ScoringRules } from '@/api/models';

/** Ordre officiel d'affichage, identique au back */
export const EVENT_TYPES = ['MS', 'WS', 'MD', 'WD', 'XD'] as const satisfies readonly EventType[];

export const EVENT_FORMATS = [
  'SINGLE_ELIMINATION',
  'POOLS_THEN_ELIMINATION',
  'ROUND_ROBIN',
  'TEAM_TIE',
] as const satisfies readonly EventFormat[];

/** Règles officielles : 2 sets gagnants de 21 points, plafond à 30 */
export const DEFAULT_SCORING: ScoringRules = { gamesToWin: 2, pointsToWin: 21, pointCap: 30 };
