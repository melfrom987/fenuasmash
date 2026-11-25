package com.tao.fenuasmash.draw;

/**
 * An entrant placed by the draw.
 *
 * @param entrant the entrant
 * @param seed its seed number, or null if not seeded
 */
record DrawnEntry(DrawEntrant entrant, Integer seed) {

	static DrawnEntry unseeded(DrawEntrant entrant) {
		return new DrawnEntry(entrant, null);
	}

}
