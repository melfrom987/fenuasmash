package com.tao.fenuasmash.draw;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.tournament.DrawFormat;

/**
 * Places the entrants of an event in pools and in a knockout bracket, following "Pools and
 * Draw" in `docs/competition-rules.md`.
 * <p>
 * The draw is reproducible: the same entrants, format, options and random seed always give
 * the same draw, so a draw can be checked or replayed later. The rules are the same as the
 * draw of the web app (`apps/web/src/features/draw/algorithm.ts`).
 */
final class DrawAlgorithm {

	static final String POOL_NAMES = "ABCDEFGHIJKLMNOP";

	static final int MIN_POOL_SIZE = 3;

	static final int MAX_POOL_SIZE = 8;

	static final int MAX_QUALIFIERS_PER_POOL = MAX_POOL_SIZE - 1;

	/** Largest random seed: 2^53 - 1, the largest integer a JavaScript client reads exactly. */
	static final long MAX_RANDOM_SEED = (1L << 53) - 1;

	/** Seeds drawn together into the same parts of the bracket: 3-4 into the quarters, etc. */
	private static final int[][] SEED_GROUPS = { { 1, 1 }, { 2, 2 }, { 3, 4 }, { 5, 8 }, { 9, 16 }, { 17, 32 } };

	private static final SecureRandom SEEDS = new SecureRandom();

	private DrawAlgorithm() {
	}

	/** A new random seed, between 0 and {@link #MAX_RANDOM_SEED}. */
	static long newRandomSeed() {
		return SEEDS.nextLong() & MAX_RANDOM_SEED;
	}

	/**
	 * Draws the entrants.
	 * @param format the format of the tournament
	 * @param entrants the confirmed entrants, in any order: they are ranked here
	 * @param options the settings, a null value using the default
	 * @param randomSeed the seed of the random draw, between 0 and {@link #MAX_RANDOM_SEED}
	 * @return the pools and the bracket
	 * @throws InvalidRequestException `NOT_ENOUGH_ENTRIES` with fewer than 2 entrants, or
	 * `INVALID_DRAW_SETTINGS` when the settings do not fit the number of entrants
	 */
	static DrawPlan draw(DrawFormat format, List<DrawEntrant> entrants, DrawOptions options, long randomSeed) {
		if (randomSeed < 0 || randomSeed > MAX_RANDOM_SEED) {
			throw invalidSettings("The random seed must be between 0 and " + MAX_RANDOM_SEED);
		}
		List<DrawEntrant> ranked = entrants.stream().sorted(DrawEntrant.RANKING).toList();
		if (ranked.size() < 2) {
			throw new InvalidRequestException("NOT_ENOUGH_ENTRIES",
					"At least 2 confirmed entries are needed for a draw");
		}
		Random random = new Random(mix(randomSeed));
		return switch (format) {
			case SINGLE_ELIMINATION -> singleElimination(ranked, options, random, randomSeed);
			case SINGLE_POOL -> pools(ranked, 1, options, random, randomSeed);
			case POOLS_THEN_ELIMINATION -> pools(ranked,
					(options.poolCount() != null) ? options.poolCount() : suggestedPoolCount(ranked.size()), options,
					random, randomSeed);
		};
	}

	private static DrawPlan singleElimination(List<DrawEntrant> ranked, DrawOptions options, Random random,
			long randomSeed) {
		Integer requested = options.seedCount();
		if (requested != null && requested < 0) {
			throw invalidSettings("The number of seeds cannot be negative");
		}
		int seeds = Math.min((requested != null) ? requested : defaultSeedCount(ranked.size()), ranked.size());
		DrawnBracket bracket = drawBracket(ranked, seeds, random);
		int seeded = (int) bracket.slots()
			.stream()
			.filter((slot) -> slot.entry() != null && slot.entry().seed() != null)
			.count();
		return new DrawPlan(List.of(), bracket, seeded, randomSeed);
	}

	private static DrawPlan pools(List<DrawEntrant> ranked, int poolCount, DrawOptions options, Random random,
			long randomSeed) {
		if (poolCount < 1 || poolCount > POOL_NAMES.length()) {
			throw invalidSettings("The number of pools must be between 1 and " + POOL_NAMES.length());
		}
		int[] sizes = poolSizes(ranked.size(), poolCount);
		int smallest = sizes[sizes.length - 1];
		int largest = sizes[0];
		boolean fits = (poolCount == 1) ? largest <= MAX_POOL_SIZE
				: smallest >= MIN_POOL_SIZE && largest <= MAX_POOL_SIZE;
		if (!fits) {
			throw invalidSettings("Pools must have " + MIN_POOL_SIZE + " to " + MAX_POOL_SIZE + " entries");
		}
		// One pool is a single pool: a round robin with no final phase
		if (poolCount == 1) {
			return new DrawPlan(drawPools(ranked, 1, 0, random), null, Math.min(2, ranked.size()), randomSeed);
		}
		int qualifiers = (options.qualifiersPerPool() != null) ? options.qualifiersPerPool() : 2;
		if (qualifiers < 1 || qualifiers >= smallest) {
			throw invalidSettings("Qualifiers per pool must be at least 1 and fewer than the smallest pool");
		}
		List<DrawnPool> pools = drawPools(ranked, poolCount, qualifiers, random);
		return new DrawPlan(pools, drawQualifierBracket(pools, random), Math.min(poolCount * 2, ranked.size()),
				randomSeed);
	}

	/** Smallest power of 2 that is at least {@code n}, and at least 2. */
	static int nextPowerOfTwo(int n) {
		int size = 2;
		while (size < n) {
			size *= 2;
		}
		return size;
	}

	/** Seeds of a single elimination: 2 up to a bracket of 8, 4 up to 16, 8 up to 32, 16 beyond. */
	static int defaultSeedCount(int entries) {
		int size = nextPowerOfTwo(entries);
		int seeds = (size <= 8) ? 2 : (size <= 16) ? 4 : (size <= 32) ? 8 : 16;
		return Math.min(seeds, entries);
	}

	/** Entries / 4, rounded, between 1 and 16. */
	static int suggestedPoolCount(int entries) {
		return Math.max(1, Math.min(POOL_NAMES.length(), Math.round(entries / 4.0f)));
	}

	/** Pool sizes, as even as possible, the largest pools first: 13 entries in 3 pools give 5, 4, 4. */
	static int[] poolSizes(int entries, int poolCount) {
		int[] sizes = new int[poolCount];
		for (int i = 0; i < poolCount; i++) {
			sizes[i] = entries / poolCount + ((i < entries % poolCount) ? 1 : 0);
		}
		return sizes;
	}

	/**
	 * The line of each seed in a standard bracket: {@code lines[seed]} is the line (from 1) of
	 * that seed, seed 1 on the first line and seed 2 on the last one.
	 */
	static int[] seedLines(int size) {
		List<Integer> standard = List.of(1);
		while (standard.size() < size) {
			int next = standard.size() * 2 + 1;
			List<Integer> doubled = new ArrayList<>();
			for (int seed : standard) {
				doubled.add(seed);
				doubled.add(next - seed);
			}
			standard = doubled;
		}
		List<Integer> order = new ArrayList<>(standard.subList(0, size / 2));
		order.addAll(standard.subList(size / 2, size).reversed());
		int[] lines = new int[size + 1];
		for (int index = 0; index < size; index++) {
			lines[order.get(index)] = index + 1;
		}
		return lines;
	}

	static List<DrawnPool> drawPools(List<DrawEntrant> ranked, int poolCount, int qualifiers, Random random) {
		int[] sizes = poolSizes(ranked.size(), poolCount);
		List<List<DrawnEntry>> pools = new ArrayList<>();
		for (int i = 0; i < poolCount; i++) {
			pools.add(new ArrayList<>());
		}
		for (int row = 0; row * poolCount < ranked.size(); row++) {
			List<DrawEntrant> rowEntries = ranked.subList(row * poolCount,
					Math.min((row + 1) * poolCount, ranked.size()));
			if (row == 0) {
				// Snake, first row: 1, 2, 3...
				for (int i = 0; i < rowEntries.size(); i++) {
					pools.get(i).add(new DrawnEntry(rowEntries.get(i), i + 1));
				}
				continue;
			}
			if (row == 1) {
				// Snake, second row, from the last pool: the first two never meet in pools
				for (int i = 0; i < rowEntries.size(); i++) {
					pools.get(poolCount - 1 - i).add(new DrawnEntry(rowEntries.get(i), poolCount + i + 1));
				}
				continue;
			}
			// Next rows at random, one entry per pool, avoiding players of the same club: the
			// entry with the fewest pools free of its clubs goes first
			List<Integer> open = new ArrayList<>();
			for (int i = 0; i < poolCount; i++) {
				if (pools.get(i).size() < sizes[i]) {
					open.add(i);
				}
			}
			List<DrawEntrant> waiting = shuffle(rowEntries, random);
			while (!waiting.isEmpty()) {
				DrawEntrant entrant = waiting.getFirst();
				long fewestOptions = Long.MAX_VALUE;
				for (DrawEntrant candidate : waiting) {
					long options = open.stream()
						.filter((pool) -> clubConflicts(pools.get(pool), candidate) == 0)
						.count();
					if (options < fewestOptions) {
						entrant = candidate;
						fewestOptions = options;
					}
				}
				int best = -1;
				long bestConflicts = Long.MAX_VALUE;
				for (int candidate : shuffle(open, random)) {
					long conflicts = clubConflicts(pools.get(candidate), entrant);
					if (conflicts < bestConflicts) {
						best = candidate;
						bestConflicts = conflicts;
					}
				}
				pools.get(best).add(DrawnEntry.unseeded(entrant));
				open.remove(Integer.valueOf(best));
				waiting.remove(entrant);
			}
		}
		List<DrawnPool> result = new ArrayList<>();
		for (int i = 0; i < poolCount; i++) {
			result.add(new DrawnPool(String.valueOf(POOL_NAMES.charAt(i)), qualifiers, pools.get(i)));
		}
		return result;
	}

	static DrawnBracket drawBracket(List<DrawEntrant> ranked, int seedCount, Random random) {
		int size = nextPowerOfTwo(ranked.size());
		Placement placement = placeSeedsAndByes(size, seedCount, size - ranked.size(), random);
		DrawnEntry[] entries = new DrawnEntry[size];
		for (int i = 0; i < placement.seedLines().size(); i++) {
			entries[placement.seedLines().get(i) - 1] = new DrawnEntry(ranked.get(i), i + 1);
		}
		List<Integer> free = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			if (!placement.byes().contains(i + 1) && entries[i] == null) {
				free.add(i);
			}
		}
		List<DrawEntrant> others = shuffle(ranked.subList(placement.seedLines().size(), ranked.size()), random);
		for (int i = 0; i < free.size(); i++) {
			entries[free.get(i)] = DrawnEntry.unseeded(others.get(i));
		}
		separateClubs(entries, free);
		List<BracketSlot> slots = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			slots.add(new BracketSlot(i + 1, entries[i], null, placement.byes().contains(i + 1)));
		}
		return new DrawnBracket(size, slots);
	}

	/**
	 * Knockout bracket of the pool qualifiers: pool winners are placed like seeds, and the
	 * other qualifiers go into the opposite half from the winner of their pool, so that two
	 * players of the same pool can only meet in the final.
	 */
	static DrawnBracket drawQualifierBracket(List<DrawnPool> pools, Random random) {
		List<PoolQualifier> qualifiers = new ArrayList<>();
		int maxRank = pools.stream().mapToInt(DrawnPool::qualifiers).max().orElse(0);
		for (int rank = 1; rank <= maxRank; rank++) {
			for (DrawnPool pool : pools) {
				if (rank <= pool.qualifiers()) {
					qualifiers.add(new PoolQualifier(pool.name(), rank));
				}
			}
		}
		int size = nextPowerOfTwo(qualifiers.size());
		List<PoolQualifier> winners = qualifiers.stream().filter((qualifier) -> qualifier.rank() == 1).toList();
		Placement placement = placeSeedsAndByes(size, winners.size(), size - qualifiers.size(), random);
		PoolQualifier[] lines = new PoolQualifier[size];
		Map<String, Integer> winnerHalf = new HashMap<>();
		for (int i = 0; i < winners.size(); i++) {
			int line = placement.seedLines().get(i);
			lines[line - 1] = winners.get(i);
			winnerHalf.put(winners.get(i).pool(), half(line, size));
		}
		List<PoolQualifier> others = qualifiers.stream().filter((qualifier) -> qualifier.rank() > 1).toList();
		for (PoolQualifier qualifier : shuffle(others, random)) {
			List<Integer> free = new ArrayList<>();
			List<Integer> opposite = new ArrayList<>();
			for (int line = 1; line <= size; line++) {
				if (lines[line - 1] == null && !placement.byes().contains(line)) {
					free.add(line);
					if (half(line, size) != winnerHalf.get(qualifier.pool())) {
						opposite.add(line);
					}
				}
			}
			List<Integer> candidates = opposite.isEmpty() ? free : opposite;
			lines[candidates.get(random.nextInt(candidates.size())) - 1] = qualifier;
		}
		List<BracketSlot> slots = new ArrayList<>();
		for (int line = 1; line <= size; line++) {
			slots.add(new BracketSlot(line, null, lines[line - 1], placement.byes().contains(line)));
		}
		return new DrawnBracket(size, slots);
	}

	/**
	 * Chooses the line of each seed and the lines of the byes.
	 * <p>
	 * Seed 1 is on the first line and seed 2 on the last one; seeds 3 and 4 are drawn into the
	 * two remaining quarters, seeds 5 to 8 into the remaining eighths, and so on. Byes go to the
	 * best seeds, on the line facing them, then to the best lines of the standard bracket.
	 */
	private static Placement placeSeedsAndByes(int size, int seedCount, int byeCount, Random random) {
		int[] standard = seedLines(size);
		List<Integer> seeds = new ArrayList<>();
		Set<Integer> byes = new HashSet<>();
		for (int[] group : SEED_GROUPS) {
			int from = group[0];
			int to = Math.min(group[1], size);
			int wanted = Math.min(to, seedCount) - from + 1;
			if (wanted <= 0) {
				break;
			}
			List<Integer> lines = new ArrayList<>();
			for (int seed = from; seed <= to; seed++) {
				if (!byes.contains(standard[seed])) {
					lines.add(standard[seed]);
				}
			}
			for (int line : shuffle(lines, random).subList(0, Math.min(wanted, lines.size()))) {
				seeds.add(line);
				int facing = facing(line);
				if (byes.size() < byeCount && !seeds.contains(facing)) {
					byes.add(facing);
				}
			}
		}
		for (int rank = 1; rank <= size && byes.size() < byeCount; rank++) {
			int line = standard[rank];
			int facing = facing(line);
			boolean taken = seeds.contains(line) || seeds.contains(facing) || byes.contains(line)
					|| byes.contains(facing);
			if (!taken) {
				byes.add(facing);
			}
		}
		return new Placement(seeds, byes);
	}

	/**
	 * Swaps unseeded entries to separate players of the same club in the first round, when
	 * possible.
	 */
	private static void separateClubs(DrawnEntry[] entries, List<Integer> movable) {
		for (int pass = 0; pass < 3; pass++) {
			for (int i : movable) {
				DrawnEntry entry = entries[i];
				DrawnEntry opponent = entries[facing(i + 1) - 1];
				if (entry == null || opponent == null || !entry.entrant().sharesClubWith(opponent.entrant())) {
					continue;
				}
				int before = firstRoundConflicts(entries);
				for (int j : movable) {
					if (j == i || j / 2 == i / 2) {
						continue;
					}
					swap(entries, i, j);
					if (firstRoundConflicts(entries) < before) {
						break;
					}
					swap(entries, i, j);
				}
			}
		}
	}

	private static long clubConflicts(List<DrawnEntry> pool, DrawEntrant entrant) {
		return pool.stream().filter((entry) -> entry.entrant().sharesClubWith(entrant)).count();
	}

	/**
	 * Spreads a random seed over the 64 bits (SplitMix64): close seeds such as 1, 2 and 3 then
	 * give unrelated draws, which {@link Random} alone does not.
	 */
	static long mix(long seed) {
		long z = seed + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** First-round matches between players of the same club. */
	static int firstRoundConflicts(DrawnEntry[] entries) {
		int count = 0;
		for (int i = 0; i + 1 < entries.length; i += 2) {
			if (entries[i] != null && entries[i + 1] != null
					&& entries[i].entrant().sharesClubWith(entries[i + 1].entrant())) {
				count++;
			}
		}
		return count;
	}

	/** The line meeting {@code line} in the first round. */
	private static int facing(int line) {
		return (line % 2 == 1) ? line + 1 : line - 1;
	}

	private static int half(int line, int size) {
		return (line <= size / 2) ? 0 : 1;
	}

	private static void swap(DrawnEntry[] entries, int i, int j) {
		DrawnEntry kept = entries[i];
		entries[i] = entries[j];
		entries[j] = kept;
	}

	/** A copy of {@code items} in random order (Fisher-Yates). */
	private static <T> List<T> shuffle(List<T> items, Random random) {
		List<T> copy = new ArrayList<>(items);
		for (int i = copy.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			T kept = copy.get(i);
			copy.set(i, copy.get(j));
			copy.set(j, kept);
		}
		return copy;
	}

	private static InvalidRequestException invalidSettings(String message) {
		return new InvalidRequestException("INVALID_DRAW_SETTINGS", message);
	}

	/**
	 * @param seedLines the line of each seed, seed 1 first
	 * @param byes the lines of the byes
	 */
	private record Placement(List<Integer> seedLines, Set<Integer> byes) {
	}

}
