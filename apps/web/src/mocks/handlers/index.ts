import { authHandlers } from './auth';
import { courtHandlers } from './courts';
import { matchHandlers } from './matches';
import { scheduleHandlers } from './schedule';
import { tabletHandlers } from './tablets';
import { organizationHandlers } from './organizations';
import { tournamentHandlers } from './tournaments';
import { eventHandlers } from './events';
import { clubHandlers } from './clubs';
import { playerHandlers } from './players';
import { registrationHandlers } from './registrations';
import { registrationImportHandlers } from './registration-import';
import { drawHandlers } from './draws';
import { seriesHandlers } from './series';
export const handlers = [
  ...authHandlers,
  ...organizationHandlers,
  ...tournamentHandlers,
  ...courtHandlers,
  ...tabletHandlers,
  ...matchHandlers,
  ...scheduleHandlers,
  ...eventHandlers,
  ...clubHandlers,
  ...playerHandlers,
  ...registrationHandlers,
  ...registrationImportHandlers,
  ...drawHandlers,
  ...seriesHandlers,
];
