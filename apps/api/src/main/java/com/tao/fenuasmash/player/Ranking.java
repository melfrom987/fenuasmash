package com.tao.fenuasmash.player;

import jakarta.validation.constraints.Min;

/**
 * A French Polynesia ranking in one discipline family; both null when the player is not
 * ranked.
 */
public record Ranking(@Min(0) Integer points, @Min(1) Integer position) {

	static final Ranking NONE = new Ranking(null, null);

}
