package com.tao.fenuasmash.match;

import static com.tao.fenuasmash.match.MatchSide.A;
import static com.tao.fenuasmash.match.MatchSide.B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.tournament.ScoringRules;

class ScoreSheetTests {

	/** 2 games of 21 points, 2 ahead, 30 at most. */
	private static final ScoringRules STANDARD = new ScoringRules(2, 21, true, 30);

	@Test
	void noRallyMeansNoGame() {
		assertThat(ScoreSheet.count(List.of(), STANDARD)).isEqualTo(ScoreSheet.EMPTY);
	}

	@Test
	void gameInProgressIsListed() {
		ScoreSheet sheet = ScoreSheet.count(rallies(5, 3), STANDARD);

		assertThat(sheet.games()).containsExactly(new GameScore(5, 3));
		assertThat(sheet.winner()).isNull();
	}

	@Test
	void gameIsWonAtTwentyOneWithTwoPointsAhead() {
		ScoreSheet sheet = ScoreSheet.count(rallies(19, 21), STANDARD);

		assertThat(sheet.games()).containsExactly(new GameScore(19, 21));
	}

	@Test
	void gameGoesOnUntilTwoPointsAhead() {
		List<MatchSide> rallies = rallies(20, 20);
		rallies.add(A);
		assertThat(ScoreSheet.count(rallies, STANDARD).games()).containsExactly(new GameScore(21, 20));

		rallies.add(A);
		assertThat(ScoreSheet.count(rallies, STANDARD).games()).containsExactly(new GameScore(22, 20));

		rallies.add(B);
		assertThat(ScoreSheet.count(rallies, STANDARD).games()).containsExactly(new GameScore(22, 20),
				new GameScore(0, 1));
	}

	@Test
	void gameIsWonAtTheCapWithOnePointAhead() {
		List<MatchSide> rallies = rallies(29, 29);
		rallies.add(B);

		assertThat(ScoreSheet.count(rallies, STANDARD).games()).containsExactly(new GameScore(29, 30));
	}

	@Test
	void withoutTwoPointLeadTheFirstSideToTheTargetWins() {
		ScoringRules rules = new ScoringRules(1, 15, false, null);
		List<MatchSide> rallies = rallies(14, 14);
		rallies.add(A);

		ScoreSheet sheet = ScoreSheet.count(rallies, rules);

		assertThat(sheet.games()).containsExactly(new GameScore(15, 14));
		assertThat(sheet.winner()).isEqualTo(A);
	}

	@Test
	void matchIsWonAtTheGamesToWin() {
		List<MatchSide> rallies = rallies(21, 15);
		rallies.addAll(rallies(18, 21));
		rallies.addAll(rallies(10, 21));

		ScoreSheet sheet = ScoreSheet.count(rallies, STANDARD);

		assertThat(sheet.games()).containsExactly(new GameScore(21, 15), new GameScore(18, 21),
				new GameScore(10, 21));
		assertThat(sheet.winner()).isEqualTo(B);
	}

	@Test
	void oneGameAheadIsNotAWin() {
		ScoreSheet sheet = ScoreSheet.count(rallies(21, 0), STANDARD);

		assertThat(sheet.games()).containsExactly(new GameScore(21, 0));
		assertThat(sheet.winner()).isNull();
	}

	@Test
	void rallyAfterTheEndOfTheMatchIsRefused() {
		List<MatchSide> rallies = rallies(21, 0);
		rallies.addAll(rallies(21, 0));
		rallies.add(B);

		assertThatIllegalArgumentException().isThrownBy(() -> ScoreSheet.count(rallies, STANDARD));
	}

	/** Side A wins its points first, then side B: the side that reaches the target last wins the game. */
	private static List<MatchSide> rallies(int a, int b) {
		List<MatchSide> rallies = new ArrayList<>();
		int min = Math.min(a, b);
		// Alternate up to the lower score, so that no game ends early
		for (int i = 0; i < min; i++) {
			rallies.add(A);
			rallies.add(B);
		}
		rallies.addAll(Collections.nCopies(a - min, A));
		rallies.addAll(Collections.nCopies(b - min, B));
		return rallies;
	}

}
