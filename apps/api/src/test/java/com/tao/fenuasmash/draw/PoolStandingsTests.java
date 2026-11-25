package com.tao.fenuasmash.draw;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.draw.PoolStandings.Result;
import com.tao.fenuasmash.draw.PoolStandings.Row;

class PoolStandingsTests {

	private static final UUID A = new UUID(0, 1);

	private static final UUID B = new UUID(0, 2);

	private static final UUID C = new UUID(0, 3);

	private static final UUID D = new UUID(0, 4);

	/** A two-game win, 21-x 21-y. */
	private static Result win(UUID winner, UUID loser, int first, int second) {
		return new Result(winner, loser, 2, 0, 42, first + second);
	}

	/** A three-game win. */
	private static Result winInThree(UUID winner, UUID loser, int points, int lost) {
		return new Result(winner, loser, 2, 1, points, lost);
	}

	private static List<UUID> order(List<Row> rows) {
		return rows.stream().map(Row::registrationId).toList();
	}

	@Test
	void ranksByMatchesWonAndCountsEverything() {
		List<Row> rows = PoolStandings.rank(List.of(A, B, C), List.of(win(C, A, 10, 10), win(C, B, 15, 15),
				win(A, B, 19, 19)));

		assertThat(order(rows)).containsExactly(C, A, B);
		assertThat(rows.getFirst()).isEqualTo(new Row(1, C, 2, 2, 0, 4, 0, 84, 50, false));
		assertThat(rows.getLast()).isEqualTo(new Row(3, B, 2, 0, 2, 0, 4, 68, 84, false));
	}

	@Test
	void ranksTheTiedEntriesAboveThoseWithFewerWins() {
		// A, B and C win 2 matches each, D none
		List<Row> rows = PoolStandings.rank(List.of(A, B, C, D),
				List.of(winInThree(B, C, 60, 55), win(B, D, 1, 1), win(C, A, 0, 0), win(C, D, 0, 0),
						win(A, B, 10, 10), win(A, D, 10, 10)));

		assertThat(rows.get(3).registrationId()).isEqualTo(D);
		assertThat(rows).extracting(Row::won).containsExactly(2, 2, 2, 0);
	}

	@Test
	void usesTheMatchBetweenTheTwoWhenOnlyTwoAreTied() {
		List<Row> rows = PoolStandings.rank(List.of(A, B, C),
				List.of(win(A, B, 20, 20), win(B, C, 0, 0), win(C, A, 0, 0), win(B, A, 0, 0)));

		// B has 2 wins, A and C 1: alone at the top. Then A and C, tied: C beat A
		assertThat(order(rows)).containsExactly(B, C, A);
	}

	@Test
	void breaksAThreeWayTieByGamesThenByTheirMatch() {
		// A, B, C beat each other in a circle; A has the best games difference (two straight wins
		// out of three games), B and C then tie on games: B beat C
		List<Row> rows = PoolStandings.rank(List.of(A, B, C), List.of(win(A, B, 15, 15),
				winInThree(B, C, 60, 50), winInThree(C, A, 60, 50)));

		assertThat(rows).extracting(Row::won).containsExactly(1, 1, 1);
		// Games: A won 2-0 and lost 1-2 (+1), C won 2-1 and lost 1-2 (0), B lost 0-2 and won 2-1 (-1)
		assertThat(order(rows)).containsExactly(A, C, B);
		assertThat(rows).noneMatch(Row::decidedByLot);
	}

	@Test
	void breaksAThreeWayTieByPointsWhenGamesAreEqual() {
		// Everyone wins once in two straight games: games are equal, points decide
		List<Row> rows = PoolStandings.rank(List.of(A, B, C),
				List.of(win(A, B, 10, 10), win(B, C, 18, 18), win(C, A, 15, 15)));

		// Points: A 42+30 / 20+42 = +10; B 42+20 / 36+42 = -16; C 42+36 / 30+42 = +6
		assertThat(order(rows)).containsExactly(A, C, B);
	}

	@Test
	void keepsTheDrawOrderAndMarksAPerfectTie() {
		List<Row> rows = PoolStandings.rank(List.of(B, A, C),
				List.of(win(A, B, 15, 15), win(B, C, 15, 15), win(C, A, 15, 15)));

		assertThat(order(rows)).containsExactly(B, A, C);
		assertThat(rows).allMatch(Row::decidedByLot);
	}

	@Test
	void ranksAPoolNotPlayedYetInTheDrawOrder() {
		List<Row> rows = PoolStandings.rank(List.of(C, A, B), List.of());

		assertThat(order(rows)).containsExactly(C, A, B);
		assertThat(rows).allMatch((row) -> row.played() == 0);
	}

}
