import type {
  Bracket,
  BracketSlot,
  Discipline,
  DrawEntry,
  DrawFormat,
  Pool,
  PoolQualifier,
  Registration,
} from '@/api/models';
import { entryPoints, sortByRanking } from '@/features/registrations/ranking';

export type Random = () => number;

export const POOL_NAMES = 'ABCDEFGHIJKLMNOP';
export const MIN_POOL_SIZE = 3;
export const MAX_POOL_SIZE = 8;

export class DrawSettingsError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

export const nextPowerOfTwo = (n: number) => 2 ** Math.ceil(Math.log2(Math.max(2, n)));

export function defaultSeedCount(entries: number) {
  const size = nextPowerOfTwo(entries);
  const seeds = size <= 8 ? 2 : size <= 16 ? 4 : size <= 32 ? 8 : 16;
  return Math.min(seeds, entries);
}

export function poolSizes(entries: number, poolCount: number) {
  const base = Math.floor(entries / poolCount);
  const extra = entries % poolCount;
  return Array.from({ length: poolCount }, (_, i) => base + (i < extra ? 1 : 0));
}

export function seriesSizes(entries: number, seriesCount: number) {
  return poolSizes(entries, seriesCount).reverse();
}

export const suggestedPoolCount = (entries: number) =>
  Math.max(1, Math.min(POOL_NAMES.length, Math.round(entries / 4)));

export function seedLines(size: number): number[] {
  let standard = [1];
  while (standard.length < size) {
    const next = standard.length * 2 + 1;
    standard = standard.flatMap((seed) => [seed, next - seed]);
  }
  const order = [...standard.slice(0, size / 2), ...standard.slice(size / 2).reverse()];
  const lines = new Array<number>(size + 1);
  order.forEach((seed, index) => {
    lines[seed] = index + 1;
  });
  return lines;
}

const shuffle = <T>(items: T[], random: Random) => {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
};

const clubsOf = (entry: DrawEntry) =>
  [entry.player.clubName, entry.partner?.clubName].filter((c): c is string => !!c);

const shareClub = (a: DrawEntry, b: DrawEntry) => {
  const clubs = clubsOf(a);
  return clubsOf(b).some((club) => clubs.includes(club));
};

export function toDrawEntries(registrations: Registration[], discipline: Discipline): DrawEntry[] {
  return sortByRanking(registrations, discipline).map((r) => ({
    registrationId: r.id,
    seed: null,
    points: entryPoints(r, discipline),
    player: r.player,
    partner: r.partner,
  }));
}

export function drawPools(
  ranked: DrawEntry[],
  poolCount: number,
  qualifiers: number,
  random: Random,
): Pool[] {
  const sizes = poolSizes(ranked.length, poolCount);
  const pools: Pool[] = sizes.map((_, i) => ({
    name: POOL_NAMES.charAt(i),
    qualifiers,
    entries: [],
  }));

  for (let tier = 0; tier * poolCount < ranked.length; tier++) {
    const tierEntries = ranked.slice(tier * poolCount, (tier + 1) * poolCount);
    if (tier === 0) {
      tierEntries.forEach((entry, i) => pools[i].entries.push({ ...entry, seed: i + 1 }));
      continue;
    }
    if (tier === 1) {
      tierEntries.forEach((entry, i) =>
        pools[poolCount - 1 - i].entries.push({ ...entry, seed: poolCount + i + 1 }),
      );
      continue;
    }
    const open = new Set(pools.map((_, i) => i).filter((i) => pools[i].entries.length < sizes[i]));
    for (const entry of shuffle(tierEntries, random)) {
      const candidates = shuffle([...open], random);
      const conflicts = (i: number) => pools[i].entries.filter((e) => shareClub(e, entry)).length;
      const best = candidates.reduce((a, b) => (conflicts(b) < conflicts(a) ? b : a));
      pools[best].entries.push(entry);
      open.delete(best);
    }
  }
  return pools;
}

function firstRoundConflicts(slots: BracketSlot[]) {
  let count = 0;
  for (let i = 0; i < slots.length; i += 2) {
    const a = slots[i].entry;
    const b = slots[i + 1].entry;
    if (a && b && shareClub(a, b)) count++;
  }
  return count;
}

function separateClubs(slots: BracketSlot[], movable: number[]) {
  for (let pass = 0; pass < 3; pass++) {
    for (const i of movable) {
      const partner = slots[i % 2 === 0 ? i + 1 : i - 1];
      const entry = slots[i].entry;
      if (!entry || !partner.entry || !shareClub(entry, partner.entry)) continue;
      const before = firstRoundConflicts(slots);
      for (const j of movable) {
        if (j === i || Math.floor(j / 2) === Math.floor(i / 2)) continue;
        [slots[i].entry, slots[j].entry] = [slots[j].entry, slots[i].entry];
        if (firstRoundConflicts(slots) < before) break;
        [slots[i].entry, slots[j].entry] = [slots[j].entry, slots[i].entry];
      }
    }
  }
}

const SEED_GROUPS = [
  [1, 1],
  [2, 2],
  [3, 4],
  [5, 8],
  [9, 16],
  [17, 32],
] as const;

function seededLineOrder(size: number, seedCount: number, random: Random) {
  const lines = seedLines(size);
  const order: number[] = [];
  for (const [from, to] of SEED_GROUPS) {
    if (from > seedCount) break;
    const group = [];
    for (let s = from; s <= Math.min(to, size); s++) group.push(lines[s]);
    order.push(...shuffle(group, random).slice(0, Math.max(0, Math.min(to, seedCount) - from + 1)));
  }
  return { lines, seedPositions: order };
}

export function drawBracket(ranked: DrawEntry[], seedCount: number, random: Random): Bracket {
  const size = nextPowerOfTwo(ranked.length);
  const byes = size - ranked.length;
  const { lines, seedPositions } = seededLineOrder(size, seedCount, random);
  const slots: BracketSlot[] = Array.from({ length: size }, (_, i) => ({
    position: i + 1,
    entry: null,
    qualifier: null,
    bye: false,
  }));

  for (let rank = size - byes + 1; rank <= size; rank++) slots[lines[rank] - 1].bye = true;
  seedPositions.forEach((position, i) => {
    slots[position - 1].entry = { ...ranked[i], seed: i + 1 };
  });

  const free = slots.map((_,i) => i).filter((i) => !slots[i].bye && !slots[i].entry);
  const others = shuffle(ranked.slice(seedPositions.length), random);
  free.forEach((index, i) => {
    slots[index].entry = others[i] ?? null;
  });
  separateClubs(slots, free);
  return { size, slots };
}

export function drawQualifierBracket(pools: Pool[], random: Random): Bracket {
  const qualifiers: PoolQualifier[] = [];
  const maxRank = Math.max(...pools.map((p) => p.qualifiers));
  for (let rank = 1; rank <= maxRank; rank++) {
    for (const pool of pools)
      if (rank <= pool.qualifiers) qualifiers.push({ pool: pool.name, rank });
  }
  const size = nextPowerOfTwo(qualifiers.length);
  const byes = size - qualifiers.length;
  const winners = qualifiers.filter((q) => q.rank === 1);
  const { lines, seedPositions } = seededLineOrder(size, winners.length, random);
  const slots: BracketSlot[] = Array.from({ length: size }, (_, i) => ({
    position: i + 1,
    entry: null,
    qualifier: null,
    bye: false,
  }));
  for (let rank = size - byes + 1; rank <= size; rank++) slots[lines[rank] - 1].bye = true;
  seedPositions.forEach((position, i) => {
    slots[position - 1].qualifier = winners[i];
  });

  const half = (position: number) => (position <= size / 2 ? 0 : 1);
  const winnerHalf = new Map(winners.map((w, i) => [w.pool, half(seedPositions[i])] as const));
  const free = slots.map((s) => s).filter((s) => !s.bye && !s.qualifier);
  for (const qualifier of shuffle(
    qualifiers.filter((q) => q.rank > 1),
    random,
  )) {
    const opposite = free.filter(
      (s) => !s.qualifier && half(s.position) !== winnerHalf.get(qualifier.pool),
    );
    const pool = opposite.length > 0 ? opposite : free.filter((s) => !s.qualifier);
    const slot = pool[Math.floor(random() * pool.length)];
    slot.qualifier = qualifier;
  }
  return { size, slots };
}

export type DrawPlan = { pools: Pool[]; bracket: Bracket | null; seedCount: number };

export function makeDraw({
  format,
  ranked,
  poolCount,
  qualifiersPerPool,
  seedCount,
  random = Math.random,
}: {
  format: DrawFormat;
  ranked: DrawEntry[];
  poolCount: number | null;
  qualifiersPerPool: number | null;
  seedCount: number | null;
  random?: Random;
}): DrawPlan {
  if (ranked.length < 2) throw new DrawSettingsError('NOT_ENOUGH_ENTRIES', 'At least 2 entries');

  if (format === 'SINGLE_ELIMINATION') {
    const seeds = Math.min(seedCount ?? defaultSeedCount(ranked.length), ranked.length);
    return { pools: [], bracket: drawBracket(ranked, seeds, random), seedCount: seeds };
  }

  const count = format === 'SINGLE_POOL' ? 1 : (poolCount ?? suggestedPoolCount(ranked.length));
  const sizes = poolSizes(ranked.length, count);
  const smallest = Math.min(...sizes);
  const largest = Math.max(...sizes);
  if (count === 1 ? largest > MAX_POOL_SIZE : smallest < MIN_POOL_SIZE || largest > MAX_POOL_SIZE) {
    throw new DrawSettingsError('INVALID_DRAW_SETTINGS', 'Pool sizes must be between 3 and 8');
  }
  if (count === 1) {
    return {
      pools: drawPools(ranked, 1, 0, random),
      bracket: null,
      seedCount: Math.min(2, ranked.length),
    };
  }
  const qualifiers = qualifiersPerPool ?? 2;
  if (qualifiers < 1 || qualifiers >= smallest) {
    throw new DrawSettingsError('INVALID_DRAW_SETTINGS', 'Qualifiers must be fewer than pool size');
  }
  const pools = drawPools(ranked, count, qualifiers, random);
  return {
    pools,
    bracket: drawQualifierBracket(pools, random),
    seedCount: Math.min(count * 2, ranked.length),
  };
}

export function clubConflicts(pools: Pool[], bracket: Bracket | null) {
  let count = 0;
  for (const pool of pools) {
    pool.entries.forEach((a, i) => {
      for (const b of pool.entries.slice(i + 1)) if (shareClub(a, b)) count++;
    });
  }
  if (bracket) count += firstRoundConflicts(bracket.slots);
  return count;
}
