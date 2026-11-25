package com.tao.fenuasmash.draw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * The ranking of a pool, with the tie-breaks of the badminton rules (BWF General Competition
 * Regulations):
 * <ol>
 * <li>most matches won;</li>
 * <li>two entries tied: the winner of their match;</li>
 * <li>three or more tied: the best difference of games won and lost, then of points; whenever
 * two of them remain tied, the winner of their match;</li>
 * <li>still tied: by lot. The order of the draw is kept, and the rows are marked.</li>
 * </ol>
 * Differences count all the matches of the pool.
 */
final class PoolStandings {

	/** The result of a match of the pool that has a winner. */
	record Result(UUID winner, UUID loser, int winnerGames, int loserGames, int winnerPoints, int loserPoints) {
	}

	/** A row of the ranking, from rank 1. */
	record Row(int rank, UUID registrationId, int played, int won, int lost, int gamesWon, int gamesLost,
			int pointsWon, int pointsLost, boolean decidedByLot) {
	}

	private PoolStandings() {
	}

	/**
	 * @param entries the entries of the pool, in the order of the draw (used for a tie by lot)
	 * @param results the matches played (or won by walkover)
	 */
	static List<Row> rank(List<UUID> entries, List<Result> results) {
		Map<UUID, int[]> totals = new LinkedHashMap<>();
		for (UUID entry : entries) {
			totals.put(entry, new int[7]);
		}
		Map<UUID, Map<UUID, UUID>> headToHead = new HashMap<>();
		for (Result result : results) {
			int[] w = totals.get(result.winner());
			int[] l = totals.get(result.loser());
			if (w == null || l == null) {
				continue;
			}
			add(w, true, result.winnerGames(), result.loserGames(), result.winnerPoints(), result.loserPoints());
			add(l, false, result.loserGames(), result.winnerGames(), result.loserPoints(), result.winnerPoints());
			headToHead.computeIfAbsent(result.winner(), (k) -> new HashMap<>()).put(result.loser(), result.winner());
			headToHead.computeIfAbsent(result.loser(), (k) -> new HashMap<>()).put(result.winner(), result.winner());
		}
		List<UUID> byWins = new ArrayList<>(entries);
		byWins.sort(Comparator.comparingInt((UUID entry) -> -totals.get(entry)[1]));
		List<UUID> order = new ArrayList<>();
		List<UUID> byLot = new ArrayList<>();
		for (List<UUID> group : groups(byWins, (entry) -> totals.get(entry)[1])) {
			order.addAll(breakTie(group, totals, headToHead, 0, byLot));
		}
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < order.size(); i++) {
			UUID entry = order.get(i);
			int[] t = totals.get(entry);
			rows.add(new Row(i + 1, entry, t[0], t[1], t[2], t[3], t[4], t[5], t[6], byLot.contains(entry)));
		}
		return rows;
	}

	private static void add(int[] totals, boolean won, int gamesWon, int gamesLost, int pointsWon, int pointsLost) {
		totals[0]++;
		totals[won ? 1 : 2]++;
		totals[3] += gamesWon;
		totals[4] += gamesLost;
		totals[5] += pointsWon;
		totals[6] += pointsLost;
	}

	/**
	 * Orders a group of entries tied so far. {@code step} 0 uses the games difference, 1 the
	 * points difference, 2 the lot.
	 */
	private static List<UUID> breakTie(List<UUID> group, Map<UUID, int[]> totals,
			Map<UUID, Map<UUID, UUID>> headToHead, int step, List<UUID> byLot) {
		if (group.size() == 1) {
			return group;
		}
		if (group.size() == 2) {
			UUID winner = headToHead.getOrDefault(group.get(0), Map.of()).get(group.get(1));
			if (winner != null) {
				return winner.equals(group.get(0)) ? group : List.of(group.get(1), group.get(0));
			}
			// Their match was not played: go on with the differences
		}
		if (step == 2) {
			byLot.addAll(group);
			return group;
		}
		ToIntFunction<UUID> difference = (step == 0) ? (entry) -> totals.get(entry)[3] - totals.get(entry)[4]
				: (entry) -> totals.get(entry)[5] - totals.get(entry)[6];
		List<UUID> sorted = new ArrayList<>(group);
		sorted.sort(Comparator.comparingInt((UUID entry) -> -difference.applyAsInt(entry)));
		List<UUID> order = new ArrayList<>();
		for (List<UUID> subgroup : groups(sorted, difference)) {
			order.addAll(breakTie(subgroup, totals, headToHead, step + 1, byLot));
		}
		return order;
	}

	/** Consecutive runs of entries with the same value, in order. */
	private static List<List<UUID>> groups(List<UUID> sorted, ToIntFunction<UUID> value) {
		List<List<UUID>> groups = new ArrayList<>();
		for (UUID entry : sorted) {
			if (groups.isEmpty()
					|| value.applyAsInt(groups.getLast().getFirst()) != value.applyAsInt(entry)) {
				groups.add(new ArrayList<>());
			}
			groups.getLast().add(entry);
		}
		return groups;
	}

}
