package com.tao.fenuasmash.match;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MatchStatusTests {

	/** The table of the contract (`changeMatchStatus`), kept in one place to compare. */
	private static final Map<MatchStatus, Set<MatchStatus>> ALLOWED = Map.of(
			MatchStatus.SCHEDULED, EnumSet.of(MatchStatus.READY, MatchStatus.CANCELLED, MatchStatus.WALKOVER),
			MatchStatus.READY, EnumSet.of(MatchStatus.SCHEDULED, MatchStatus.CALLED, MatchStatus.CANCELLED,
					MatchStatus.WALKOVER),
			MatchStatus.CALLED, EnumSet.of(MatchStatus.READY, MatchStatus.WARMUP, MatchStatus.CANCELLED,
					MatchStatus.WALKOVER),
			MatchStatus.WARMUP, EnumSet.of(MatchStatus.CALLED, MatchStatus.IN_PROGRESS, MatchStatus.CANCELLED,
					MatchStatus.WALKOVER),
			MatchStatus.IN_PROGRESS, EnumSet.of(MatchStatus.FINISHED),
			MatchStatus.FINISHED, EnumSet.of(MatchStatus.IN_PROGRESS),
			MatchStatus.CANCELLED, EnumSet.noneOf(MatchStatus.class),
			MatchStatus.WALKOVER, EnumSet.noneOf(MatchStatus.class));

	@Test
	void onlyTheTransitionsOfTheContractAreAllowed() {
		for (MatchStatus from : MatchStatus.values()) {
			for (MatchStatus to : MatchStatus.values()) {
				assertThat(from.canMoveTo(to)).as(from + " -> " + to).isEqualTo(ALLOWED.get(from).contains(to));
			}
		}
	}

	@Test
	void noStatusMovesToItself() {
		for (MatchStatus status : MatchStatus.values()) {
			assertThat(status.canMoveTo(status)).as(status.name()).isFalse();
		}
	}

	@Test
	void cancelledAndWalkoverAreFinal() {
		assertThat(MatchStatus.CANCELLED.nextStatuses()).isEmpty();
		assertThat(MatchStatus.WALKOVER.nextStatuses()).isEmpty();
	}

	@Test
	void onlyScheduledAndCancelledMatchesMayMissASide() {
		for (MatchStatus status : MatchStatus.values()) {
			assertThat(status.needsBothSides()).as(status.name())
				.isEqualTo(status != MatchStatus.SCHEDULED && status != MatchStatus.CANCELLED);
		}
	}

}
