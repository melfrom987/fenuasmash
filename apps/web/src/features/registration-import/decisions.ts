import type { PlayerDecision } from './types';

export type Decisions = {
  players: Record<number, PlayerDecision>;
  partners: Record<string, PlayerDecision>;
};

export const partnerKey = (rowNumber: number, eventId: string) => `${rowNumber}:${eventId}`;
