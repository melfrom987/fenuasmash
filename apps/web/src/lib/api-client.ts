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
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(
      res.status,
      body?.detail ?? `HTTP ${res.status}`,
      body?.code,
      Array.isArray(body?.errors) ? body.errors : [],
    );
  }
  return body as T;
}
