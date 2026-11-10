package com.tao.fenuasmash.tournament;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateScoringRulesRequest(
		@Min(1) @Max(3) Integer gamesToWin,
		@Min(1) @Max(50) Integer pointsToWin,
		Boolean twoPointLead,
		@Min(1) @Max(50) Integer pointCap) {
}
