package com.tao.fenuasmash.tournament;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoringRulesRequest(
		@NotNull @Min(1) @Max(3) Integer gamesToWin,
		@NotNull @Min(1) @Max(50) Integer pointsToWin,
		@NotNull Boolean twoPointLead,
		@Min(1) @Max(50) Integer pointCap) {
}
