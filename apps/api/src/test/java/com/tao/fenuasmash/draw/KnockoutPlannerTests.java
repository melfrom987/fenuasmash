package com.tao.fenuasmash.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.match.KnockoutMatch;

class KnockoutPlannerTests {

	private static final UUID A = new UUID(0, 1);

	private static final UUID B = new UUID(0, 2);

	private static final UUID C = new UUID(0, 3);

	private static final UUID D = new UUID(0, 4);

	@Test
	void playsAFinalWithTwoEntries() {
		assertThat(KnockoutPlanner.plan(List.of(A, B))).containsExactly(new KnockoutMatch(1, 1, A, B));
	}

	@Test
	void chainsTheRoundsOfAFullBracket() {
		assertThat(KnockoutPlanner.plan(List.of(A, B, C, D))).containsExactly(new KnockoutMatch(1, 1, A, B),
				new KnockoutMatch(1, 2, C, D), new KnockoutMatch(2, 1, null, null));
	}

	@Test
	void sendsAnEntryFacingAByeToTheSecondRound() {
		// Three entries: A faces a bye and waits in the final for the winner of C and D
		List<UUID> lines = Arrays.asList(A, null, C, D);

		assertThat(KnockoutPlanner.plan(lines)).containsExactly(new KnockoutMatch(1, 2, C, D),
				new KnockoutMatch(2, 1, A, null));
	}

	@Test
	void placesByesOnBothSidesOfTheSecondRound() {
		// Bracket of 8 with 3 byes: lines 2, 7 and 4 are byes
		List<UUID> lines = Arrays.asList(A, null, B, null, C, D, null, new UUID(0, 5));

		List<KnockoutMatch> matches = KnockoutPlanner.plan(lines);

		assertThat(matches).filteredOn((match) -> match.round() == 1)
			.containsExactly(new KnockoutMatch(1, 3, C, D));
		assertThat(matches).filteredOn((match) -> match.round() == 2)
			.containsExactly(new KnockoutMatch(2, 1, A, B), new KnockoutMatch(2, 2, null, new UUID(0, 5)));
		assertThat(matches).filteredOn((match) -> match.round() == 3)
			.containsExactly(new KnockoutMatch(3, 1, null, null));
	}

	@Test
	void makesOneMatchLessThanEntriesWhateverTheirNumber() {
		Random random = new Random(7);
		for (int entries = 2; entries <= 64; entries++) {
			List<UUID> registrations = new ArrayList<>();
			for (int i = 0; i < entries; i++) {
				registrations.add(new UUID(1, i));
			}
			DrawnBracket bracket = DrawAlgorithm.drawBracket(registrations.stream()
				.map((id) -> new DrawEntrant(id, null, java.time.Instant.EPOCH, java.util.Set.of()))
				.toList(), DrawAlgorithm.defaultSeedCount(entries), random);
			List<UUID> lines = bracket.slots()
				.stream()
				.map((slot) -> (slot.entry() != null) ? slot.entry().entrant().registrationId() : null)
				.toList();

			List<KnockoutMatch> matches = KnockoutPlanner.plan(lines);

			int rounds = Integer.numberOfTrailingZeros(bracket.size());
			assertThat(matches).as("%d entries", entries).hasSize(entries - 1);
			assertThat(matches.getLast()).isEqualTo(new KnockoutMatch(rounds, 1,
					matches.getLast().sideA(), matches.getLast().sideB()));
			// Every entry plays its first match once, in round 1 or 2
			assertThat(matches.stream()
				.flatMap((match) -> java.util.stream.Stream.of(match.sideA(), match.sideB()))
				.filter(Objects::nonNull)
				.toList()).as("%d entries", entries).containsExactlyInAnyOrderElementsOf(registrations);
			for (int round = 1; round <= rounds; round++) {
				int r = round;
				List<Integer> positions = matches.stream()
					.filter((match) -> match.round() == r)
					.map(KnockoutMatch::position)
					.toList();
				assertThat(positions).doesNotHaveDuplicates().allMatch((p) -> p >= 1 && p <= bracket.size() >> r);
			}
		}
	}

	@Test
	void refusesABracketThatIsNotAPowerOfTwoOrTwoByesFacingEachOther() {
		assertThatThrownBy(() -> KnockoutPlanner.plan(List.of(A, B, C))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> KnockoutPlanner.plan(Arrays.asList(A, B, null, null)))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
