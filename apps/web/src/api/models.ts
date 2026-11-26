export type IsoDate = string;

export type Discipline = 'MS' | 'WS' | 'MD' | 'WD' | 'XD' | 'OD';
export type CategoryType = 'OPEN' | 'AGE' | 'LEVEL';

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
  averageMatchMinutes?: number;
  convocationLeadMinutes?: number;
  publicVisibility?: PublicVisibility;
};

export type PublicVisibility = 'ON_PUBLISH' | 'AT_START';

export type Gender = 'MALE' | 'FEMALE';

export type Club = {
  id: string;
  name: string;
  city: string;
  address: string | null;
  email: string | null;
  phone: string | null;
  logoUrl: string | null;
};

export type ClubSummary = Pick<Club, 'id' | 'name' | 'logoUrl'>;

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
  birthDate: string;
  club: ClubSummary;
  licenseNumber: string | null;
  email: string | null;
  phone: string | null;
  rankings: PlayerRankings;
};

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
  plannedEndDate?: IsoDate | null;
  thirdPlaceMatch?: boolean;
  maxEntries: number | null;
  minBirthYear: number | null;
  maxBirthYear: number | null;
  poolCount: number | null;
  poolSize: number | null;
  qualifiersPerPool: number | null;
  parentEventId: string | null;
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

export type PlayerSummary = Pick<
  Player,
  'id' | 'firstName' | 'lastName' | 'gender' | 'birthDate' | 'licenseNumber' | 'rankings'
> & { clubName: string };

export type Registration = {
  id: string;
  tournamentId: string;
  eventId: string;
  status: RegistrationStatus;
  player: PlayerSummary;
  partner: PlayerSummary | null;
  registeredAt: IsoDate;
  createdAt: IsoDate;
};

export type DrawStatus = 'DRAFT' | 'PUBLISHED';

export type DrawEntry = {
  registrationId: string;
  seed: number | null;
  points: number | null;
  player: PlayerSummary;
  partner: PlayerSummary | null;
};

export type Pool = {
  name: string;
  qualifiers: number;
  entries: DrawEntry[];
};

export type PoolQualifier = { pool: string; rank: number };

export type BracketSlot = {
  position: number;
  entry: DrawEntry | null;
  qualifier: PoolQualifier | null;
  bye: boolean;
};

export type Bracket = {
  size: number;
  slots: BracketSlot[];
};

export type Draw = {
  eventId: string;
  status: DrawStatus;
  seedCount: number;
  pools: Pool[];
  bracket: Bracket | null;
  excludedRegistrationIds: string[];
  createdAt: IsoDate;
  publishedAt: IsoDate | null;
};

export type DrawSettings = {
  poolCount: number | null;
  qualifiersPerPool: number | null;
  seedCount: number | null;
};

export type DrawSlotRef = { pool: string; index: number } | { position: number };

export type SeriesSplit = { label: string; registrationIds: string[] };
export type SeriesSplitRequest = { series: SeriesSplit[] };
export type SeriesSplitResult = { events: Event[] };

export type UserRole = 'ADMIN' | 'ORGANIZER' | 'CLUB' | 'PLAYER';
export type LoginRequest = { email: string; password: string };
export type LoginResponse = { accessToken: string; tokenType: 'Bearer'; expiresAt: IsoDate };
export type CurrentUser = {
  id: string;
  email: string;
  role: UserRole;
  organizationId: string | null;
};

export type PairingCode = { code: string; courtId: string; expiresAt: IsoDate };
export type Tablet = {
  id: string;
  courtId: string;
  courtNumber: number;
  deviceName: string | null;
  pairedAt: IsoDate;
  lastSeenAt: IsoDate;
  revokedAt: IsoDate | null;
};
export type PairTabletResponse = {
  tabletId: string;
  accessToken: string;
  tokenType: 'Bearer';
  court: Court;
};

export type MatchStatus =
  | 'SCHEDULED'
  | 'READY'
  | 'CALLED'
  | 'WARMUP'
  | 'IN_PROGRESS'
  | 'FINISHED'
  | 'CANCELLED'
  | 'WALKOVER';
export type MatchSide = 'A' | 'B';
export type MatchOutcome = 'NORMAL' | 'RETIRED' | 'WALKOVER';
export type Match = {
  id: string;
  tournamentId: string;
  eventId: string;
  round: number | null;
  sideA: string | null;
  sideB: string | null;
  courtId: string | null;
  status: MatchStatus;
  winner: MatchSide | null;
  outcome: MatchOutcome | null;
  stateVersion: number;
  startedAt: IsoDate | null;
  endedAt: IsoDate | null;
  createdAt: IsoDate;
  updatedAt: IsoDate;
  number?: number | null;
  phase?: MatchPhase | null;
  pool?: string | null;
  sourceA?: MatchSource | null;
  sourceB?: MatchSource | null;
  plannedStartAt?: IsoDate | null;
  plannedCourtId?: string | null;
};
export type MatchPhase = 'POOL' | 'KNOCKOUT' | 'THIRD_PLACE';
export type MatchSource =
  | { kind: 'POOL_RANK'; pool: string; rank: number }
  | { kind: 'WINNER'; matchId: string }
  | { kind: 'LOSER'; matchId: string };
export type QueuedMatch = { match: Match; available: boolean };
export type CourtAssignment = { court: Court; match: Match | null };
export type CreateMatchRequest = {
  round: number | null;
  sideA: string | null;
  sideB: string | null;
};
export type ChangeMatchStatusRequest = {
  status: MatchStatus;
  stateVersion: number;
  winner?: MatchSide | null;
  outcome?: 'NORMAL' | 'RETIRED' | null;
};

export type ScheduleStatus = 'NOT_GENERATED' | 'DRAFT' | 'PUBLISHED';
export type ScheduleWarning =
  | { code: 'NO_SLOT'; eventId: string }
  | { code: 'OUTSIDE_SLOT'; eventId: string; count: number }
  | { code: 'REST_TOO_SHORT'; matchId: string }
  | { code: 'ORDER'; matchId: string }
  | { code: 'UNSCHEDULED'; count: number };
export type ScheduleStage = 'KNOCKOUT' | 'QUARTER' | 'SEMI' | 'FINAL';
export type ScheduleStageStart = { stage: ScheduleStage; at: IsoDate };
export type ScheduleEventSetting = {
  eventId: string;
  offsetSlots: number;
  startAt?: IsoDate | null;
  stageStarts?: ScheduleStageStart[];
};
export type ScheduleEvent = ScheduleEventSetting & {
  matchCount: number;
  firstStartAt: IsoDate | null;
  lastEndAt: IsoDate | null;
};
export type ScheduleGenerateRequest = { events?: ScheduleEventSetting[] };
export type Schedule = {
  tournamentId: string;
  status: ScheduleStatus;
  events: ScheduleEvent[];
  generatedAt: IsoDate | null;
  publishedAt: IsoDate | null;
  firstStartAt: IsoDate | null;
  lastEndAt: IsoDate | null;
  warnings: ScheduleWarning[];
};
export type ScheduleMoveRequest = { matchId: string; plannedStartAt: IsoDate; courtId: string };
