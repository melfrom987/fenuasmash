package com.tao.fenuasmash.draw;

import java.util.List;

/**
 * The knockout bracket of the draw.
 *
 * @param size the number of lines, a power of 2
 * @param slots the lines, in order
 */
record DrawnBracket(int size, List<BracketSlot> slots) {

	DrawnBracket {
		slots = List.copyOf(slots);
	}

}
