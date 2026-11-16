import { useEffect, useState } from 'react';

/** Renvoie `value` une fois qu'elle n'a plus changé pendant `delay` ms (ex. : recherche à la frappe). */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
