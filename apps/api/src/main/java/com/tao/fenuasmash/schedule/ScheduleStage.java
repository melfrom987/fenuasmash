package com.tao.fenuasmash.schedule;

/**
 * Later stages of an event that can be held back: every knockout match, quarter-finals and
 * later, semi-finals and final, or the final. The third place match follows every stage.
 */
public enum ScheduleStage {

	KNOCKOUT(Integer.MAX_VALUE), QUARTER(2), SEMI(1), FINAL(0);

	/** Rounds before the last one that the stage holds. */
	private final int depth;

	ScheduleStage(int depth) {
		this.depth = depth;
	}

	int depth() {
		return depth;
	}

}
