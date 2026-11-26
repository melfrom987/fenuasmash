package com.tao.fenuasmash.draw;

import java.util.List;

/**
 * A pool of the draw.
 *
 * @param name A, B, C...
 * @param qualifiers how many entries of the pool go to the knockout bracket, 0 for a single pool
 * @param entries the entries, best seeds first
 */
record DrawnPool(String name, int qualifiers, List<DrawnEntry> entries) {

	DrawnPool {
		entries = List.copyOf(entries);
	}

}
