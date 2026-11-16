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
  disciplines?: Discipline[];
  forbiddenCombinations?: [Discipline, Discipline][];
};

export type Gender = 'MALE' | 'FEMALE';

export type Club = {
  id: string;
  name: string;
  city: string;
  address: string | null;
  email: string | null;
  phone: string | null;
  /** Fixé par l'envoi du logo (PUT /clubs/{id}/logo), jamais dans le JSON */
  logoUrl: string | null;
};

/** Club tel qu'il apparaît dans un joueur */
export type ClubSummary = Pick<Club, 'id' | 'name' | 'logoUrl'>;

/** Classement de Polynésie dans une discipline (null = pas classé) */
export type Ranking = {
  points: number | null;
  position: number | null;
};

export type PlayerRankings = {
  singles: Ranking;
  doubles: Ranking;
  mixed: Ranking;
};

export type Player = {
  id: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  /** Date seule, sans heure : YYYY-MM-DD */
  birthDate: string;
  club: ClubSummary;
  licenseNumber: string | null;
  email: string | null;
  phone: string | null;
  rankings: PlayerRankings;
};

/** Une page de résultats (page à partir de 0) */
export type PlayerPage = {
  items: Player[];
  page: number;
  size: number;
  totalItems: number;
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

export type RegistrationStatus = 'CONFIRMED' | 'PENDING_PARTNER' | 'LOOKING_FOR_PARTNER';

/** Joueur tel qu'il apparaît dans une inscription */
export type PlayerSummary = Pick<
  Player,
  'id' | 'firstName' | 'lastName' | 'gender' | 'birthDate' | 'licenseNumber'
> & { clubName: string };

export type Registration = {
  id: string;
  tournamentId: string;
  eventId: string;
  status: RegistrationStatus;
  player: PlayerSummary;
  /** Null en simple, et en double quand le joueur cherche un partenaire */
  partner: PlayerSummary | null;
  createdAt: IsoDate;
};
