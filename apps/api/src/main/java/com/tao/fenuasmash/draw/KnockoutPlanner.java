package com.tao.fenuasmash.draw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tao.fenuasmash.match.KnockoutMatch;
import com.tao.fenuasmash.match.MatchSource;
import com.tao.fenuasmash.match.MatchSide;

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
	 * The match and side where the entry of a line plays first: in round 1, or in round 2
	 * when it faces a bye.
	 */
	record Slot(int round, int position, MatchSide side) {
	}

	static Slot firstSlot(List<Line> lines, int line) {
		int position = (line + 1) / 2;
		boolean facingBye = lines.get((line % 2 == 1) ? line : line - 2).bye();
		if (!facingBye) {
			return new Slot(1, position, (line % 2 == 1) ? MatchSide.A
					: MatchSide.B);
		}
		return new Slot(2, (position + 1) / 2, (position % 2 == 1) ? MatchSide.A
				: MatchSide.B);
	}

	/**
	 * A line of the first round: a bye, or an entry, known or not yet (a pool qualifier).
	 *
	 * @param qualifier the pool rank the line waits for, or null
	 */
	record Line(UUID registration, boolean bye, MatchSource qualifier) {

		Line(UUID registration, boolean bye) {
			this(registration, bye, null);
		}

		static Line of(UUID registration) {
			return (registration != null) ? new Line(registration, false) : new Line(null, true);
		}

	}

	/**
	 * @param lines the registration on each line of the bracket, from line 1, null for a bye
	 */
	static List<KnockoutMatch> plan(List<UUID> lines) {
		return planLines(lines.stream().map(Line::of).toList());
	}

	/** The matches of a bracket whose lines may still wait for their entry (a pool qualifier). */
	static List<KnockoutMatch> planLines(List<Line> lines) {
		int size = lines.size();
		if (size < 2 || Integer.bitCount(size) != 1) {
			throw new IllegalArgumentException("A bracket has a power of 2 lines, not " + size);
		}
		int rounds = Integer.numberOfTrailingZeros(size);
		// Sides of the second round already known, from the byes of the first one
		UUID[] secondRound = new UUID[size / 2];
		MatchSource[] secondRoundSources = new MatchSource[size / 2];
		List<KnockoutMatch> matches = new ArrayList<>();
		for (int position = 1; position <= size / 2; position++) {
			Line a = lines.get(2 * position - 2);
			Line b = lines.get(2 * position - 1);
			if (a.bye() && b.bye()) {
				throw new IllegalArgumentException("Two byes face each other at position " + position);
			}
			if (!a.bye() && !b.bye()) {
				matches.add(new KnockoutMatch(1, position, a.registration(), b.registration(), a.qualifier(),
						b.qualifier()));
			}
			else {
				Line through = a.bye() ? b : a;
				secondRound[position - 1] = through.registration();
				secondRoundSources[position - 1] = through.qualifier();
			}
		}
		for (int round = 2; round <= rounds; round++) {
			int count = size >> round;
			for (int position = 1; position <= count; position++) {
				UUID a = (round == 2) ? secondRound[2 * position - 2] : null;
				UUID b = (round == 2) ? secondRound[2 * position - 1] : null;
				MatchSource sourceA = (round == 2) ? secondRoundSources[2 * position - 2] : null;
				MatchSource sourceB = (round == 2) ? secondRoundSources[2 * position - 1] : null;
				matches.add(new KnockoutMatch(round, position, a, b, sourceA, sourceB));
			}
		}
		return matches;
	}

}
