package com.tao.fenuasmash.draw;

import java.util.List;

/**
 * Result of a draw.
 *
 * @param pools the pools, empty for a single elimination
 * @param bracket the knockout bracket, or null for a single pool
 * @param seedCount the number of seeded entries
 * @param randomSeed the seed of the random draw: the same entrants, settings and seed always
 * give the same draw
 */
record DrawPlan(List<DrawnPool> pools, DrawnBracket bracket, int seedCount, long randomSeed) {

	DrawPlan {
		pools = List.copyOf(pools);
	}

}
