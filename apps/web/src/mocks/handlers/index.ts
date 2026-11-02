import { organizationHandlers } from './organizations';
import { tournamentHandlers } from './tournaments';

export const handlers = [...organizationHandlers, ...tournamentHandlers];
