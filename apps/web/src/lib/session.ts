const STORAGE_KEY = 'fenuasmash.session';

export const UNAUTHORIZED_EVENT = 'fenuasmash:unauthorized';

export type Session = { accessToken: string; expiresAt: string };

let current: Session | null | undefined;

const isValid = (session: Session) =>
  typeof session.accessToken === 'string' && Date.parse(session.expiresAt) > Date.now();

function load(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function readSession(): Session | null {
  if (current === undefined) current = load();
  if (current && !isValid(current)) clearSession();
  return current ?? null;
}

export function saveSession(session: Session) {
  current = session;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    return;
  }
}

export function clearSession() {
  current = null;
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    return;
  }
}

export function notifyUnauthorized() {
  clearSession();
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}
