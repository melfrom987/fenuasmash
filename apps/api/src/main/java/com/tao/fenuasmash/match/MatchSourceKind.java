package com.tao.fenuasmash.match;

/** Where an unknown side of a match comes from. */
public enum MatchSourceKind {

	/** A final rank in a pool, e.g. the winner of pool A. */
	POOL_RANK,

	/** The winner of another match. */
	WINNER,

	/** The loser of another match. */
	LOSER

}
