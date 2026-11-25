package com.tao.fenuasmash.match;

import java.util.ArrayList;
import java.util.List;

import com.tao.fenuasmash.tournament.ScoringRules;

/**
 * The score of a match, counted from the rallies won by each side with the
 * scoring rules of the tournament.
 * <p>
 * A game is won at {@code pointsToWin} with a two-point lead, or at
 * {@code pointCap} (21 points, 2 ahead, 30 at most, in the standard rules).
 * Without {@code twoPointLead}, the first side at {@code pointsToWin} wins it.
 * The match is won at {@code gamesToWin} games.
 *
 * @param games each game played or in progress, in order
 * @param winner the side that won the match; null while nobody has
 */
record ScoreSheet(List<GameScore> games, MatchSide winner) {

	static final ScoreSheet EMPTY = new ScoreSheet(List.of(), null);

	/**
	 * @param rallies the side that won each rally, in order
	 * @throws IllegalArgumentException when a rally comes after the end of the match
	 */
	static ScoreSheet count(List<MatchSide> rallies, ScoringRules rules) {
		List<GameScore> games = new ArrayList<>();
		int a = 0;
		int b = 0;
		int gamesA = 0;
		int gamesB = 0;
		MatchSide winner = null;
		for (MatchSide side : rallies) {
			if (winner != null) {
				throw new IllegalArgumentException("A rally after the end of the match");
			}
			if (side == MatchSide.A) {
				a++;
			}
			else {
				b++;
			}
			if (isGameWon(a, b, rules)) {
				games.add(new GameScore(a, b));
				a = 0;
				b = 0;
				gamesA += side == MatchSide.A ? 1 : 0;
				gamesB += side == MatchSide.B ? 1 : 0;
				if (Math.max(gamesA, gamesB) == rules.gamesToWin()) {
					winner = side;
				}
			}
		}
		if (a + b > 0) {
			games.add(new GameScore(a, b));
		}
		return new ScoreSheet(List.copyOf(games), winner);
	}

	private static boolean isGameWon(int a, int b, ScoringRules rules) {
		int leader = Math.max(a, b);
		if (!rules.twoPointLead()) {
			return leader >= rules.pointsToWin();
		}
		boolean capReached = rules.pointCap() != null && leader >= rules.pointCap();
		return capReached || (leader >= rules.pointsToWin() && Math.abs(a - b) >= 2);
	}

}
