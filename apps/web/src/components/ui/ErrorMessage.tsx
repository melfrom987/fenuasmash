import type { ReactNode } from 'react';

export function ErrorMessage({ children }: { children: ReactNode }) {
  return <p className="rounded-md bg-red-100 px-3 py-2 text-red-800">{children}</p>;
}
