import { courtHandlers } from './courts';
import { organizationHandlers } from './organizations';
import { tournamentHandlers } from './tournaments';
import { eventHandlers } from './events';
import { clubHandlers } from './clubs';
import { playerHandlers } from './players';
import { registrationHandlers } from './registrations';
// ...
export const handlers = [
  ...organizationHandlers,
  ...tournamentHandlers,
  ...courtHandlers,
  ...eventHandlers,
  ...clubHandlers,
  ...playerHandlers,
  ...registrationHandlers,
];
