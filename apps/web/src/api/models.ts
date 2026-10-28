export type IsoDate = string;

export type EventType = 'MS' | 'WS' | 'MD' | 'WD' | 'XD';
export type MatchStatus =
  | 'PENDING'
  | 'READY'
  | 'CALLED'
  | 'WARMUP'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'WALKOVER';
export type AssignmentStatus = 'ASSIGNED' | 'RELEASED' | 'CANCELLED';
export type DeviceStatus = 'ACTIVE' | 'REVOKED';

export type Organization = {
  id: string;
  name: string;
  createdAt: IsoDate;
};

export type Tournament = {
  id: string;
  organizationId: string;
  name: string;
  startsAt: IsoDate;
  endsAt: IsoDate;
  minimumRestMinutes: number;
  createdAt: IsoDate;
  updatedAt: IsoDate;
};

export type Player = {
  id: string;
  firstName: string;
  lastName: string;
  externalReference: string | null;
  createdAt: IsoDate;
};

export type Event = {
  id: string;
  tournamentId: string;
  name: string;
  type: EventType;
};

export type Entry = {
  id: string;
  eventId: string;
};

// Entry avec ses joueurs (include: { players: { include: { player: true } } })
export type EntryWithPlayers = Entry & {
  players: { entryId: string; playerId: string; player: Player }[];
};

export type Court = {
  id: string;
  tournamentId: string;
  label: string;
  active: boolean;
};

export type Match = {
  id: string;
  tournamentId: string;
  eventId: string;
  reference: string | null;
  roundLabel: string | null;
  entryAId: string | null;
  entryBId: string | null;
  status: MatchStatus;
  scheduledAt: IsoDate | null;
  startedAt: IsoDate | null;
  finishedAt: IsoDate | null;
  score: unknown;
  stateVersion: number;
};

export type MatchAssignment = {
  id: string;
  matchId: string;
  courtId: string;
  status: AssignmentStatus;
  assignedAt: IsoDate;
  releasedAt: IsoDate | null;
};

// Version publique d'un Device : sans tokenHash
export type Device = {
  id: string;
  courtId: string;
  label: string | null;
  status: DeviceStatus;
  pairedAt: IsoDate;
  revokedAt: IsoDate | null;
  lastSeenAt: IsoDate | null;
};