import type { Gender, PlayerSummary, RegistrationStatus } from '@/api/models';

export type Cell = string | Date;

export type Table = {
  fileName: string;
  headers: string[];
  rows: Cell[][];
};

export const FIELDS = [
  'fullName',
  'birthDate',
  'gender',
  'club',
  'email',
  'phone',
  'registeredAt',
] as const;
export type Field = (typeof FIELDS)[number];
export const REQUIRED_FIELDS: readonly Field[] = ['fullName', 'birthDate', 'gender', 'club'];

export type NameOrder = 'LAST_FIRST' | 'FIRST_LAST';
export type DateOrder = 'DMY' | 'MDY';

export type EventColumn = { column: number; partnerColumn: number | null };

export type ColumnMapping = {
  fields: Record<Field, number | null>;
  events: EventColumn[];
  nameOrder: NameOrder;
  dateOrder: DateOrder;
};

export const IGNORE = 'IGNORE';

export type ValueMapping = {
  events: Record<number, Record<string, string>>;
  genders: Record<string, Gender>;
  clubCities: Record<string, string>;
};

export type PlayerDecision =
  { action: 'USE_EXISTING'; playerId: string } | { action: 'CREATE_NEW' };

export type ImportPartner =
  { kind: 'NONE' } | { kind: 'LOOKING' } | { kind: 'NAMED'; name: string };

export type ImportEntry = {
  eventId: string;
  partner: ImportPartner;
  partnerDecision?: PlayerDecision;
};

export type ImportRow = {
  rowNumber: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: Gender;
  clubName: string;
  clubCity: string | null;
  email: string | null;
  phone: string | null;
  playerDecision?: PlayerDecision;
  registeredAt: string | null;
  entries: ImportEntry[];
};

export type MatchResult = 'EXACT' | 'PROBABLE' | 'AMBIGUOUS' | 'NONE';

export type MatchReason =
  | 'SAME_NAME'
  | 'SIMILAR_NAME'
  | 'SAME_BIRTH_DATE'
  | 'DIFFERENT_BIRTH_DATE'
  | 'SAME_CLUB'
  | 'OTHER_ROW_OF_FILE';

export type PlayerCandidate = { player: PlayerSummary; score: number; reasons: MatchReason[] };

export type PlayerMatch = {
  result: MatchResult;
  selectedPlayerId: string | null;
  candidates: PlayerCandidate[];
};

export type ImportIssue = { code: string; message: string };

export type ImportEntryReport = {
  eventId: string;
  status: RegistrationStatus | null;
  partnerMatch: PlayerMatch | null;
  issues: ImportIssue[];
};

export type ImportRowReport = {
  rowNumber: number;
  imported: boolean;
  playerMatch: PlayerMatch;
  entries: ImportEntryReport[];
  issues: ImportIssue[];
};

export type ImportReport = {
  dryRun: boolean;
  summary: {
    rows: number;
    rowsWithIssues: number;
    playersMatched: number;
    playersToCreate: number;
    playersToConfirm: number;
    confirmed: number;
    pendingPartner: number;
    lookingForPartner: number;
  };
  rows: ImportRowReport[];
};
