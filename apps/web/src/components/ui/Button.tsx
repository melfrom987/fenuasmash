import type { ButtonHTMLAttributes } from 'react';

export function Button({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={`bg-lagon-600 hover:bg-lagon-700 inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
      {...props}
    />
  );
}
