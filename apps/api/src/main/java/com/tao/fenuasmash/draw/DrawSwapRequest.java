package com.tao.fenuasmash.draw;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The `DrawSwapRequest` schema. */
record DrawSwapRequest(@NotNull @Valid SlotRef a, @NotNull @Valid SlotRef b) {

	/** A pool slot ({@code pool} and {@code index}, from 0) or a bracket line ({@code position}, from 1). */
	record SlotRef(String pool, Integer index, Integer position) {

		boolean isPoolSlot() {
			return pool != null && index != null && position == null;
		}

		boolean isBracketLine() {
			return position != null && pool == null && index == null;
		}

	}

}
