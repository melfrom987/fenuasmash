import { courtHandlers } from './courts';
import { organizationHandlers } from './organizations';
import { tournamentHandlers } from './tournaments';
import { eventHandlers } from './events';
// ...
export const handlers = [
  ...organizationHandlers,
  ...tournamentHandlers,
  ...courtHandlers,
  ...eventHandlers,
];
