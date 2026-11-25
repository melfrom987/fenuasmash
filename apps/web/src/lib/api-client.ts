import { notifyUnauthorized, readSession } from './session';

export type FieldError = { field: string; message: string };

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, message: string, code?: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const isFormData = init?.body instanceof FormData;
  const session = readSession();
  const headers = new Headers(init?.headers);
  if (!isFormData && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (session) headers.set('Authorization', `Bearer ${session.accessToken}`);

  const res = await fetch(`/api${path}`, { ...init, headers });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && session) notifyUnauthorized();
    throw new ApiError(
      res.status,
      body?.detail ?? `HTTP ${res.status}`,
      body?.code,
      Array.isArray(body?.errors) ? body.errors : [],
    );
  }
  return body as T;
}
