import type { NameOrder } from './types';

const isUpperWord = (word: string) =>
  word.length > 1 && word === word.toUpperCase() && word !== word.toLowerCase();

export function splitFullName(
  fullName: string,
  order: NameOrder,
): { lastName: string; firstName: string } {
  const words = fullName.trim().split(/\s+/).filter(Boolean);
  if (words.length < 2) return { lastName: words[0] ?? '', firstName: '' };

  const upper = words.filter(isUpperWord);
  if (upper.length > 0 && upper.length < words.length) {
    return {
      lastName: upper.join(' '),
      firstName: words.filter((w) => !isUpperWord(w)).join(' '),
    };
  }
  return order === 'LAST_FIRST'
    ? { lastName: words[0], firstName: words.slice(1).join(' ') }
    : { lastName: words[words.length - 1], firstName: words.slice(0, -1).join(' ') };
}
