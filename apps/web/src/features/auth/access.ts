import type { UserRole } from '@/api/models';

export const ADMIN_ROLES: readonly UserRole[] = ['ADMIN', 'ORGANIZER'];

export const DEFAULT_REDIRECT = '/admin';

export function safeRedirect(value: string | null) {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/login')) {
    return DEFAULT_REDIRECT;
  }
  return value;
}

export function loginPath(redirect: string, expired = false) {
  const params = new URLSearchParams({ redirect });
  if (expired) params.set('expired', '1');
  return `/login?${params.toString()}`;
}
