package com.tao.fenuasmash.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.tournament.DrawFormat;

class DrawAlgorithmTests {

	private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");

	/**
	 * Entrants ranked 1 to {@code count}: the first has the most points. Each one is in its own
	 * club, unless {@code clubs} gives it one.
	 */
	private static List<DrawEntrant> ranked(int count, Map<Integer, String> clubs) {
		List<DrawEntrant> entrants = new ArrayList<>();
		for (int rank = 1; rank <= count; rank++) {
			entrants.add(new DrawEntrant(new UUID(0, rank), 10_000.0 - rank, START.plusSeconds(rank),
					Set.of(clubs.getOrDefault(rank, "Club " + rank))));
		}
		return entrants;
	}

	private static List<DrawEntrant> ranked(int count) {
		return ranked(count, Map.of());
	}

	/** Rank (from 1) of an entrant made by {@link #ranked}. */
	private static int rank(DrawnEntry entry) {
		return (int) entry.entrant().registrationId().getLeastSignificantBits();
	}

	private static DrawPlan draw(DrawFormat format, List<DrawEntrant> entrants, DrawOptions options, long seed) {
		List<DrawEntrant> shuffled = new ArrayList<>(entrants);
		Collections.shuffle(shuffled, new Random(seed));
		return DrawAlgorithm.draw(format, shuffled, options, seed);
	}

	private static DrawPlan knockout(int entrants, Integer seedCount, long seed) {
		return draw(DrawFormat.SINGLE_ELIMINATION, ranked(entrants), new DrawOptions(null, null, seedCount), seed);
	}

	/** Line (from 1) of the entrant of rank {@code rank}, or -1. */
	private static int lineOf(DrawnBracket bracket, int rank) {
		return bracket.slots()
			.stream()
			.filter((slot) -> slot.entry() != null && rank(slot.entry()) == rank)
			.mapToInt(BracketSlot::position)
			.findFirst()
			.orElse(-1);
	}

	private static BracketSlot facing(DrawnBracket bracket, int line) {
		int other = (line % 2 == 1) ? line + 1 : line - 1;
		return bracket.slots().get(other - 1);
	}

	private static String codeOf(Runnable draw) {
		try {
			draw.run();
		}
		catch (InvalidRequestException ex) {
			return ex.getCode();
		}
		throw new AssertionError("No error");
	}

	@Test
	void ranksByPointsThenRegistrationDate() {
		DrawEntrant unranked = new DrawEntrant(new UUID(0, 1), null, START, Set.of("A"));
		DrawEntrant best = new DrawEntrant(new UUID(0, 2), 500.0, START.plusSeconds(9), Set.of("B"));
		DrawEntrant earlier = new DrawEntrant(new UUID(0, 3), 200.0, START.plusSeconds(1), Set.of("C"));
		DrawEntrant later = new DrawEntrant(new UUID(0, 4), 200.0, START.plusSeconds(2), Set.of("D"));

		List<DrawEntrant> sorted = new ArrayList<>(List.of(unranked, later, best, earlier));
		sorted.sort(DrawEntrant.RANKING);

		assertThat(sorted).containsExactly(best, earlier, later, unranked);
	}

	@Test
	void placesStandardSeedLines() {
		int[] lines = DrawAlgorithm.seedLines(8);

		assertThat(lines[1]).isEqualTo(1);
		assertThat(lines[2]).isEqualTo(8);
		// Seeds 3 and 4 in the two middle quarters, seeds 5 to 8 facing seeds 1 to 4
		assertThat(Set.of(lines[3], lines[4])).containsExactlyInAnyOrder(3, 6);
		assertThat(IntStream.rangeClosed(1, 8).map((seed) -> lines[seed]).boxed()).containsExactlyInAnyOrder(1, 2,
				3, 4, 5, 6, 7, 8);
	}

	@Test
	void usesTheDefaultsOfTheContract() {
		assertThat(DrawAlgorithm.defaultSeedCount(8)).isEqualTo(2);
		assertThat(DrawAlgorithm.defaultSeedCount(9)).isEqualTo(4);
		assertThat(DrawAlgorithm.defaultSeedCount(32)).isEqualTo(8);
		assertThat(DrawAlgorithm.defaultSeedCount(33)).isEqualTo(16);
		assertThat(DrawAlgorithm.defaultSeedCount(1)).isEqualTo(1);
		assertThat(DrawAlgorithm.suggestedPoolCount(4)).isEqualTo(1);
		assertThat(DrawAlgorithm.suggestedPoolCount(13)).isEqualTo(3);
		assertThat(DrawAlgorithm.suggestedPoolCount(100)).isEqualTo(16);
		assertThat(DrawAlgorithm.poolSizes(13, 3)).containsExactly(5, 4, 4);
	}

	@Nested
	class SingleElimination {

		@Test
		void placesEveryEntrantOnce() {
			for (int entrants = DrawAlgorithm.MIN_ENTRIES; entrants <= 40; entrants++) {
				DrawnBracket bracket = knockout(entrants, null, entrants).bracket();

				assertThat(bracket.size()).isEqualTo(DrawAlgorithm.nextPowerOfTwo(entrants));
				assertThat(bracket.slots().stream().filter((slot) -> slot.entry() != null).map((slot) -> rank(slot.entry())))
					.as("%d entrants", entrants)
					.containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, entrants).boxed().toList());
				assertThat(bracket.slots().stream().filter(BracketSlot::bye).count())
					.isEqualTo(bracket.size() - entrants);
			}
		}

		@Test
		void putsSeedOneFirstAndSeedTwoLast() {
			for (long seed = 0; seed < 20; seed++) {
				DrawnBracket bracket = knockout(16, null, seed).bracket();

				assertThat(lineOf(bracket, 1)).isEqualTo(1);
				assertThat(lineOf(bracket, 2)).isEqualTo(16);
				assertThat(Set.of(lineOf(bracket, 3), lineOf(bracket, 4))).containsExactlyInAnyOrder(5, 12);
			}
		}

		@Test
		void drawsSeedsThreeAndFourIntoTheMiddleQuarters() {
			Set<Integer> linesOfSeedThree = IntStream.range(0, 40)
				.mapToObj((seed) -> lineOf(knockout(16, null, seed).bracket(), 3))
				.collect(Collectors.toSet());

			// Drawn at random: both quarters are used
			assertThat(linesOfSeedThree).containsExactlyInAnyOrder(5, 12);
		}

		@Test
		void numbersTheSeeds() {
			DrawPlan plan = knockout(16, null, 1);

			assertThat(plan.seedCount()).isEqualTo(4);
			assertThat(plan.bracket().slots())
				.filteredOn((slot) -> slot.entry() != null && slot.entry().seed() != null)
				.extracting((slot) -> slot.entry().seed(), (slot) -> rank(slot.entry()))
				.containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(1, 1),
						org.assertj.core.groups.Tuple.tuple(2, 2), org.assertj.core.groups.Tuple.tuple(3, 3),
						org.assertj.core.groups.Tuple.tuple(4, 4));
		}

		@Test
		void givesByesToTheBestSeeds() {
			for (long seed = 0; seed < 20; seed++) {
				// 13 entrants: 3 byes, 4 seeds
				DrawnBracket bracket = knockout(13, null, seed).bracket();

				for (int rank = 1; rank <= 3; rank++) {
					assertThat(facing(bracket, lineOf(bracket, rank)).bye()).as("seed %d", rank).isTrue();
				}
				assertThat(facing(bracket, lineOf(bracket, 4)).bye()).isFalse();
			}
		}

		@Test
		void givesByesToSeedsDrawnInTheSameGroup() {
			for (long seed = 0; seed < 40; seed++) {
				// 5 entrants, all seeded: 3 byes for seeds 1, 2 and 3, wherever seed 3 is drawn
				DrawnBracket bracket = knockout(5, 5, seed).bracket();

				for (int rank = 1; rank <= 3; rank++) {
					assertThat(facing(bracket, lineOf(bracket, rank)).bye()).as("seed %d", rank).isTrue();
				}
			}
		}

		@Test
		void neverPutsTwoByesFacingEachOther() {
			for (int entrants = 3; entrants <= 33; entrants++) {
				DrawnBracket bracket = knockout(entrants, 0, entrants).bracket();

				for (BracketSlot slot : bracket.slots()) {
					if (slot.bye()) {
						assertThat(facing(bracket, slot.position()).entry()).as("%d entrants", entrants).isNotNull();
					}
				}
			}
		}

		@Test
		void separatesPlayersOfTheSameClubInTheFirstRound() {
			// 8 entrants, two by two of the same club
			Map<Integer, String> clubs = Map.of(1, "X", 2, "X", 3, "Y", 4, "Y", 5, "Z", 6, "Z", 7, "W", 8, "W");
			for (long seed = 0; seed < 20; seed++) {
				DrawPlan plan = draw(DrawFormat.SINGLE_ELIMINATION, ranked(8, clubs), DrawOptions.DEFAULTS, seed);
				DrawnEntry[] lines = plan.bracket().slots().stream().map(BracketSlot::entry).toArray(DrawnEntry[]::new);

				assertThat(DrawAlgorithm.firstRoundConflicts(lines)).isZero();
			}
		}

		@Test
		void capsTheSeedsAtTheNumberOfEntrants() {
			assertThat(knockout(3, 10, 1).seedCount()).isEqualTo(3);
		}

	}

	@Nested
	class Pools {

		private DrawPlan pools(int entrants, Integer poolCount, Integer qualifiers, long seed) {
			return draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(entrants),
					new DrawOptions(poolCount, qualifiers, null), seed);
		}

		private Map<String, List<Integer>> ranksByPool(DrawPlan plan) {
			return plan.pools()
				.stream()
				.collect(Collectors.toMap(DrawnPool::name,
						(pool) -> pool.entries().stream().map(DrawAlgorithmTests::rank).toList()));
		}

		@Test
		void splitsTheEntrantsEvenly() {
			DrawPlan plan = pools(13, 3, null, 1);

			assertThat(plan.pools()).extracting(DrawnPool::name).containsExactly("A", "B", "C");
			assertThat(plan.pools()).extracting((pool) -> pool.entries().size()).containsExactly(5, 4, 4);
			assertThat(plan.pools()).allSatisfy((pool) -> assertThat(pool.qualifiers()).isEqualTo(2));
		}

		@Test
		void fillsTheFirstTwoRowsInASnake() {
			for (long seed = 0; seed < 10; seed++) {
				Map<String, List<Integer>> pools = ranksByPool(pools(13, 3, null, seed));

				assertThat(pools.get("A").subList(0, 2)).containsExactly(1, 6);
				assertThat(pools.get("B").subList(0, 2)).containsExactly(2, 5);
				assertThat(pools.get("C").subList(0, 2)).containsExactly(3, 4);
			}
		}

		@Test
		void putsOneAndFourTogetherWithTwoPools() {
			Map<String, List<Integer>> pools = ranksByPool(pools(8, 2, null, 3));

			assertThat(pools.get("A").subList(0, 2)).containsExactly(1, 4);
			assertThat(pools.get("B").subList(0, 2)).containsExactly(2, 3);
		}

		@Test
		void drawsTheNextRowsAtRandom() {
			Set<String> poolsOfRankSeven = IntStream.range(0, 40)
				.mapToObj((seed) -> ranksByPool(pools(12, 3, null, seed)))
				.map((pools) -> pools.entrySet()
					.stream()
					.filter((pool) -> pool.getValue().contains(7))
					.map(Map.Entry::getKey)
					.findFirst()
					.orElseThrow())
				.collect(Collectors.toSet());

			assertThat(poolsOfRankSeven).containsExactlyInAnyOrder("A", "B", "C");
		}

		@Test
		void separatesPlayersOfTheSameClub() {
			// Ranks 5 and 7 are drawn at random in two different rows, and are of the same club
			Map<Integer, String> clubs = Map.of(5, "X", 7, "X");
			for (long seed = 0; seed < 20; seed++) {
				DrawPlan plan = draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(8, clubs), new DrawOptions(2, null, null),
						seed);

				assertThat(plan.pools()).allSatisfy((pool) -> assertThat(
						pool.entries().stream().filter((entry) -> entry.entrant().clubs().contains("X")).count())
					.isLessThanOrEqualTo(1));
			}
		}

		@Test
		void seedsTheFirstTwoRows() {
			DrawPlan plan = pools(12, 3, null, 1);

			assertThat(plan.seedCount()).isEqualTo(6);
			assertThat(plan.pools().get(0).entries())
				.extracting(DrawnEntry::seed)
				.containsExactly(1, 6, null, null);
		}

		@Test
		void placesPoolWinnersLikeSeedsAndRunnersUpInTheOppositeHalf() {
			for (long seed = 0; seed < 20; seed++) {
				DrawnBracket bracket = pools(16, 4, 2, seed).bracket();
				Map<PoolQualifier, Integer> lines = bracket.slots()
					.stream()
					.filter((slot) -> slot.qualifier() != null)
					.collect(Collectors.toMap(BracketSlot::qualifier, BracketSlot::position));

				assertThat(bracket.size()).isEqualTo(8);
				assertThat(lines).hasSize(8);
				assertThat(lines.get(new PoolQualifier("A", 1))).isEqualTo(1);
				assertThat(lines.get(new PoolQualifier("B", 1))).isEqualTo(8);
				for (String pool : List.of("A", "B", "C", "D")) {
					boolean winnerTop = lines.get(new PoolQualifier(pool, 1)) <= 4;
					boolean runnerUpTop = lines.get(new PoolQualifier(pool, 2)) <= 4;
					assertThat(runnerUpTop).as("pool %s", pool).isNotEqualTo(winnerTop);
				}
			}
		}

		@Test
		void givesTheByesOfTheBracketToTheBestPoolWinners() {
			// 3 pools of 4, 2 qualifiers: 6 qualifiers in a bracket of 8, 2 byes
			DrawnBracket bracket = pools(12, 3, 2, 5).bracket();
			Function<PoolQualifier, Integer> lineOf = (qualifier) -> bracket.slots()
				.stream()
				.filter((slot) -> qualifier.equals(slot.qualifier()))
				.mapToInt(BracketSlot::position)
				.findFirst()
				.orElseThrow();

			assertThat(facing(bracket, lineOf.apply(new PoolQualifier("A", 1))).bye()).isTrue();
			assertThat(facing(bracket, lineOf.apply(new PoolQualifier("B", 1))).bye()).isTrue();
			assertThat(bracket.slots().stream().filter(BracketSlot::bye).count()).isEqualTo(2);
		}

		@Test
		void makesASinglePoolWithoutBracket() {
			DrawPlan single = draw(DrawFormat.SINGLE_POOL, ranked(5), DrawOptions.DEFAULTS, 1);
			DrawPlan onePool = pools(5, null, null, 1);

			for (DrawPlan plan : List.of(single, onePool)) {
				assertThat(plan.pools()).hasSize(1);
				assertThat(plan.pools().get(0).entries()).hasSize(5);
				assertThat(plan.pools().get(0).qualifiers()).isZero();
				assertThat(plan.bracket()).isNull();
			}
		}

	}

	@Nested
	class Reproducibility {

		@Test
		void givesTheSameDrawForTheSameSeed() {
			Map<Integer, String> clubs = Map.of(3, "X", 9, "X", 14, "X");
			for (DrawFormat format : DrawFormat.values()) {
				int entrants = (format == DrawFormat.SINGLE_POOL) ? 8 : 20;
				DrawPlan first = draw(format, ranked(entrants, clubs), DrawOptions.DEFAULTS, 42);
				DrawPlan again = draw(format, ranked(entrants, clubs), DrawOptions.DEFAULTS, 42);

				assertThat(again).as(format.name()).isEqualTo(first);
				assertThat(first.randomSeed()).isEqualTo(42);
			}
		}

		@Test
		void ignoresTheOrderOfTheEntrants() {
			List<DrawEntrant> entrants = ranked(20);
			List<DrawEntrant> reversed = new ArrayList<>(entrants.reversed());

			assertThat(DrawAlgorithm.draw(DrawFormat.SINGLE_ELIMINATION, reversed, DrawOptions.DEFAULTS, 7))
				.isEqualTo(DrawAlgorithm.draw(DrawFormat.SINGLE_ELIMINATION, entrants, DrawOptions.DEFAULTS, 7));
		}

		@Test
		void givesAnotherDrawForAnotherSeed() {
			Set<DrawPlan> plans = IntStream.range(0, 10)
				.mapToObj((seed) -> knockout(20, null, seed))
				.map((plan) -> new DrawPlan(plan.pools(), plan.bracket(), plan.seedCount(), 0))
				.collect(Collectors.toSet());

			assertThat(plans).hasSizeGreaterThan(1);
		}

		@Test
		void makesRandomSeedsAJavaScriptClientReadsExactly() {
			for (int i = 0; i < 100; i++) {
				assertThat(DrawAlgorithm.newRandomSeed()).isBetween(0L, DrawAlgorithm.MAX_RANDOM_SEED);
			}
		}

	}

	@Nested
	class Errors {

		@Test
		void needsThreeEntrants() {
			assertThat(codeOf(() -> knockout(2, null, 1))).isEqualTo("NOT_ENOUGH_ENTRIES");
			assertThat(codeOf(() -> knockout(1, null, 1))).isEqualTo("NOT_ENOUGH_ENTRIES");
			assertThat(knockout(3, null, 1).bracket().size()).isEqualTo(4);
			assertThat(codeOf(() -> DrawAlgorithm.draw(DrawFormat.SINGLE_POOL, List.of(), DrawOptions.DEFAULTS, 1)))
				.isEqualTo("NOT_ENOUGH_ENTRIES");
		}

		@Test
		void refusesPoolsOutsideThreeToEight() {
			assertThat(codeOf(() -> draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(7), new DrawOptions(4, null, null),
					1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(codeOf(() -> draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(18), new DrawOptions(2, null, null),
					1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(codeOf(() -> draw(DrawFormat.SINGLE_POOL, ranked(9), DrawOptions.DEFAULTS, 1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(codeOf(() -> draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(40), new DrawOptions(17, null, null),
					1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
		}

		@Test
		void needsFewerQualifiersThanTheSmallestPool() {
			assertThat(codeOf(() -> draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(12), new DrawOptions(3, 4, null), 1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(codeOf(() -> draw(DrawFormat.POOLS_THEN_ELIMINATION, ranked(12), new DrawOptions(3, 0, null), 1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
		}

		@Test
		void refusesANegativeSeedCount() {
			assertThat(codeOf(() -> knockout(8, -1, 1))).isEqualTo("INVALID_DRAW_SETTINGS");
		}

		@Test
		void refusesARandomSeedOutOfRange() {
			assertThat(codeOf(() -> knockout(8, null, -1))).isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(codeOf(() -> knockout(8, null, DrawAlgorithm.MAX_RANDOM_SEED + 1)))
				.isEqualTo("INVALID_DRAW_SETTINGS");
			assertThat(Objects.requireNonNull(knockout(8, null, DrawAlgorithm.MAX_RANDOM_SEED).bracket())).isNotNull();
		}

	}

}
