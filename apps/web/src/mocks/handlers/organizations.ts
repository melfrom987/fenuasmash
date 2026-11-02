import { delay, http, HttpResponse } from 'msw';
import { db } from '../data';

export const organizationHandlers = [
  http.get('/api/organizations', async () => {
    await delay();
    return HttpResponse.json(db.organizations);
  }),
];
