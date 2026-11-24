import { delay, http, HttpResponse } from 'msw';
import type { CurrentUser, LoginRequest } from '@/api/models';
import { db } from '../data';
import { problem } from './utils';

const SESSION_MINUTES = 60;
const MAX_FAILED_LOGINS = 5;

type MockAccount = CurrentUser & { password: string };

const accounts: MockAccount[] = [
  {
    id: '90000000-0000-4000-8000-000000000001',
    email: 'admin@fenuasmash.pf',
    password: 'admin',
    role: 'ADMIN',
    organizationId: null,
  },
  {
    id: '90000000-0000-4000-8000-000000000002',
    email: 'orga@fenuasmash.pf',
    password: 'orga',
    role: 'ORGANIZER',
    organizationId: db.organizations[0]?.id ?? null,
  },
  {
    id: '90000000-0000-4000-8000-000000000003',
    email: 'club@fenuasmash.pf',
    password: 'club',
    role: 'CLUB',
    organizationId: null,
  },
];

const failedLogins = new Map<string, number>();
const TOKEN_PREFIX = 'mock-token.';

const unauthorized = () => problem(401, 'UNAUTHORIZED', 'Missing, invalid or expired token');

function accountFrom(request: Request) {
  const header = request.headers.get('Authorization') ?? '';
  const token = header.replace(/^Bearer /, '');
  if (!token.startsWith(TOKEN_PREFIX)) return undefined;
  const [id, expiresAt] = token.slice(TOKEN_PREFIX.length).split('.');
  if (Number(expiresAt) < Date.now()) return undefined;
  return accounts.find((a) => a.id === id);
}

const toCurrentUser = (account: MockAccount): CurrentUser => ({
  id: account.id,
  email: account.email,
  role: account.role,
  organizationId: account.organizationId,
});

export const authHandlers = [
  http.post('/api/auth/login', async ({ request }) => {
    await delay();
    const { email = '', password = '' } = (await request.json()) as Partial<LoginRequest>;
    const normalized = email.trim().toLowerCase();
    if (!normalized || !password) {
      return problem(400, 'VALIDATION_FAILED', 'Invalid request');
    }
    if ((failedLogins.get(normalized) ?? 0) >= MAX_FAILED_LOGINS) {
      return problem(429, 'TOO_MANY_LOGIN_ATTEMPTS', 'Too many failed logins');
    }
    const account = accounts.find((a) => a.email === normalized && a.password === password);
    if (!account) {
      failedLogins.set(normalized, (failedLogins.get(normalized) ?? 0) + 1);
      return problem(401, 'INVALID_CREDENTIALS', 'Wrong email or password');
    }
    failedLogins.delete(normalized);
    const expires = Date.now() + SESSION_MINUTES * 60 * 1000;
    return HttpResponse.json({
      accessToken: `${TOKEN_PREFIX}${account.id}.${expires}`,
      tokenType: 'Bearer',
      expiresAt: new Date(expires).toISOString(),
    });
  }),

  http.get('/api/auth/me', async ({ request }) => {
    await delay();
    const account = accountFrom(request);
    return account ? HttpResponse.json(toCurrentUser(account)) : unauthorized();
  }),

  http.all('/api/*', ({ request }) => {
    if (new URL(request.url).pathname === '/api/tablets/pair') return undefined;
    if (!accountFrom(request)) return unauthorized();
    return undefined;
  }),
];
