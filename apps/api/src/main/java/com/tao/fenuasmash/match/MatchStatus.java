package com.tao.fenuasmash.match;

import java.util.Set;

/**
 * Where a match stands, and which statuses it may move to.
 */
public enum MatchStatus {

	/** To be played; a side may still be unknown. */
	SCHEDULED,

	/** Both sides are known and available, waiting for a court. */
	READY,

	/** The players are called to the court. */
	CALLED,

	/** The players warm up on the court. */
	WARMUP,

	/** Being played. */
	IN_PROGRESS,

	/** Won on the court, or by retirement of the loser during the match. */
	FINISHED,

	/** Will not be played, without a winner. Final. */
	CANCELLED,

	/** Won by forfeit before the start. Final. */
	WALKOVER;

	/**
	 * Statuses this one may move to. A match can step back before it starts (for
	 * example a called player is not there yet), and a finished match can be
	 * reopened to undo its last point.
	 */
	Set<MatchStatus> nextStatuses() {
		return switch (this) {
			case SCHEDULED -> Set.of(READY, CANCELLED, WALKOVER);
			case READY -> Set.of(SCHEDULED, CALLED, CANCELLED, WALKOVER);
			case CALLED -> Set.of(READY, WARMUP, CANCELLED, WALKOVER);
			case WARMUP -> Set.of(CALLED, IN_PROGRESS, CANCELLED, WALKOVER);
			case IN_PROGRESS -> Set.of(FINISHED);
			case FINISHED -> Set.of(IN_PROGRESS);
			case CANCELLED, WALKOVER -> Set.of();
		};
	}

	boolean canMoveTo(MatchStatus next) {
		return nextStatuses().contains(next);
	}

	/** Every status except these needs both sides of the match. */
	boolean needsBothSides() {
		return this != SCHEDULED && this != CANCELLED;
	}

	/** The match is over: no status change will start it again, except reopening a finished match. */
	boolean isEnded() {
		return this == FINISHED || this == CANCELLED || this == WALKOVER;
	}

}
