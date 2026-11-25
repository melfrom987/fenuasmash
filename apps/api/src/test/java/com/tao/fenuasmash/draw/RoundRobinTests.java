package com.tao.fenuasmash.draw;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RoundRobinTests {

	private static List<UUID> entries(int count) {
		List<UUID> entries = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			entries.add(new UUID(0, i));
		}
		return entries;
	}

	@Test
	void everyoneMeetsEveryoneOnceAndPlaysOnceARound() {
		for (int size = 2; size <= 8; size++) {
			List<UUID> entries = entries(size);

			List<RoundRobin.Pairing> pairings = RoundRobin.schedule(entries);

			assertThat(pairings).as("%d entries", size).hasSize(size * (size - 1) / 2);
			Set<Set<UUID>> pairs = pairings.stream()
				.map((pairing) -> Set.of(pairing.sideA(), pairing.sideB()))
				.collect(Collectors.toSet());
			assertThat(pairs).as("%d entries: no pair twice", size).hasSize(pairings.size());
			int rounds = (size % 2 == 0) ? size - 1 : size;
			Map<Integer, List<RoundRobin.Pairing>> byRound = pairings.stream()
				.collect(Collectors.groupingBy(RoundRobin.Pairing::round));
			assertThat(byRound.keySet()).hasSize(rounds);
			for (List<RoundRobin.Pairing> round : byRound.values()) {
				Set<UUID> playing = new HashSet<>();
				for (RoundRobin.Pairing pairing : round) {
					assertThat(playing.add(pairing.sideA())).isTrue();
					assertThat(playing.add(pairing.sideB())).isTrue();
				}
				assertThat(playing).hasSize(size - size % 2);
			}
		}
	}

	@Test
	void doesNotAlwaysPutTheFirstEntryOnSideA() {
		List<RoundRobin.Pairing> pairings = RoundRobin.schedule(entries(4));

		assertThat(pairings).anyMatch((pairing) -> pairing.sideB().equals(new UUID(0, 1)));
	}

}
