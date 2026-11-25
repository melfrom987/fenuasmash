package com.tao.fenuasmash.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** One ranking per discipline family. */
public record PlayerRankings(@NotNull @Valid Ranking singles, @NotNull @Valid Ranking doubles,
		@NotNull @Valid Ranking mixed) {

	static final PlayerRankings NONE = new PlayerRankings(Ranking.NONE, Ranking.NONE, Ranking.NONE);

}
