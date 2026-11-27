package com.tao.fenuasmash.match;

import java.util.UUID;

/**
 * A match of a knockout bracket, before it is created.
 *
 * @param round the round, from 1
 * @param position the position in the round, from 1 (top of the bracket): the winners of
 * positions 2k-1 and 2k meet at position k of the next round, as sides A and B
 * @param sideA the registration of side A, or null until it is known
 * @param sideB the registration of side B, or null until it is known
 * @param sourceA the pool rank side A waits for, or null; the winners of earlier matches are
 * added when the matches are created
 * @param sourceB the pool rank side B waits for, or null
 */
public record KnockoutMatch(int round, int position, UUID sideA, UUID sideB, MatchSource sourceA,
		MatchSource sourceB) {

	public KnockoutMatch(int round, int position, UUID sideA, UUID sideB) {
		this(round, position, sideA, sideB, null, null);
	}

}
