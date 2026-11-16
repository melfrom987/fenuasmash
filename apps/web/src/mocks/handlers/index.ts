import { courtHandlers } from './courts';
import { organizationHandlers } from './organizations';
import { tournamentHandlers } from './tournaments';
import { eventHandlers } from './events';
import { clubHandlers } from './clubs';
import { playerHandlers } from './players';
// ...
export const handlers = [
  ...organizationHandlers,
  ...tournamentHandlers,
  ...courtHandlers,
  ...eventHandlers,
  ...clubHandlers,
  ...playerHandlers,
];
