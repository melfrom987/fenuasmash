package com.tao.fenuasmash.draw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tao.fenuasmash.match.KnockoutMatch;

/**
 * The matches of a single elimination, from the first round of its bracket.
 * <p>
 * Lines 2k-1 and 2k meet in the first round. An entry facing a bye plays no first-round
 * match: it starts in the second round, where it is already placed. Every later match is
 * created with the sides known so far; the others come from the winners.
 */
final class KnockoutPlanner {

	private KnockoutPlanner() {
	}

	/**
	 * @param lines the registration on each line of the bracket, from line 1, null for a bye
	 */
	static List<KnockoutMatch> plan(List<UUID> lines) {
		int size = lines.size();
		if (size < 2 || Integer.bitCount(size) != 1) {
			throw new IllegalArgumentException("A bracket has a power of 2 lines, not " + size);
		}
		int rounds = Integer.numberOfTrailingZeros(size);
		// Sides of the second round already known, from the byes of the first one
		UUID[] secondRound = new UUID[size / 2];
		List<KnockoutMatch> matches = new ArrayList<>();
		for (int position = 1; position <= size / 2; position++) {
			UUID a = lines.get(2 * position - 2);
			UUID b = lines.get(2 * position - 1);
			if (a == null && b == null) {
				throw new IllegalArgumentException("Two byes face each other at position " + position);
			}
			if (a != null && b != null) {
				matches.add(new KnockoutMatch(1, position, a, b));
			}
			else {
				secondRound[position - 1] = (a != null) ? a : b;
			}
		}
		for (int round = 2; round <= rounds; round++) {
			int count = size >> round;
			for (int position = 1; position <= count; position++) {
				UUID a = (round == 2) ? secondRound[2 * position - 2] : null;
				UUID b = (round == 2) ? secondRound[2 * position - 1] : null;
				matches.add(new KnockoutMatch(round, position, a, b));
			}
		}
		return matches;
	}

}
