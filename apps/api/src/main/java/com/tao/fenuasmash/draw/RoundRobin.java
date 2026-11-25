package com.tao.fenuasmash.draw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The matches of a pool: everyone meets everyone once, spread over rounds so that nobody
 * plays twice in a round (circle method). With an odd number of entries, one entry rests in
 * each round.
 */
final class RoundRobin {

	/** A match of the pool, in round {@code round} (from 1). */
	record Pairing(int round, UUID sideA, UUID sideB) {
	}

	private RoundRobin() {
	}

	static List<Pairing> schedule(List<UUID> entries) {
		List<UUID> circle = new ArrayList<>(entries);
		if (circle.size() % 2 == 1) {
			// The entry facing the gap rests
			circle.add(null);
		}
		int n = circle.size();
		List<Pairing> pairings = new ArrayList<>();
		for (int round = 1; round < n; round++) {
			for (int i = 0; i < n / 2; i++) {
				UUID a = circle.get(i);
				UUID b = circle.get(n - 1 - i);
				if (a != null && b != null) {
					// Alternates sides, so that the first entry is not always side A
					pairings.add((round % 2 == 1 || i > 0) ? new Pairing(round, a, b) : new Pairing(round, b, a));
				}
			}
			// The first entry stays, the others turn by one place
			circle.add(1, circle.remove(n - 1));
		}
		return pairings;
	}

}
