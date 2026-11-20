package com.tao.fenuasmash.match;

/**
 * How a match was won.
 */
public enum MatchOutcome {

	/** Played to the end. */
	NORMAL,

	/** The loser retired during the match. */
	RETIRED,

	/** The loser forfeited before the start. */
	WALKOVER

}
