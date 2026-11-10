export type IsoDate = string;

export type Discipline = 'MS' | 'WS' | 'MD' | 'WD' | 'XD' | 'OD';
export type CategoryType = 'OPEN' | 'AGE' | 'LEVEL';
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
  competitionType?: CompetitionType;
  drawFormat?: DrawFormat;
  playMode?: PlayMode;
  scoring?: ScoringRules;
  disciplines?: Discipline[]; // dans l'ordre où elles se jouent
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
  discipline: Discipline;
  categoryType: CategoryType;
  categoryLabel: string | null;
  plannedDate: IsoDate | null;
  maxEntries: number | null;
  poolCount: number | null;
  poolSize: number | null;
  qualifiersPerPool: number | null;
};
export type Entry = {
  id: string;
  eventId: string;
};

export type EntryWithPlayers = Entry & {
  players: { entryId: string; playerId: string; player: Player }[];
};

export type Court = {
  id: string;
  tournamentId: string;
  number: number;
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

export type Device = {
  id: string;
  courtId: string;
  label: string | null;
  status: DeviceStatus;
  pairedAt: IsoDate;
  revokedAt: IsoDate | null;
  lastSeenAt: IsoDate | null;
};

export type CompetitionType = 'INDIVIDUAL' | 'TEAM';
export type DrawFormat = 'SINGLE_ELIMINATION' | 'POOLS_THEN_ELIMINATION' | 'SINGLE_POOL';
export type PlayMode = 'SETS' | 'RELAY';

export type ScoringRules = {
  gamesToWin: number;
  pointsToWin: number;
  twoPointLead: boolean;
  pointCap: number | null;
};
