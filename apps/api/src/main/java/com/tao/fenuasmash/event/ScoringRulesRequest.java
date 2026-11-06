package com.tao.fenuasmash.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ScoringRulesRequest(
		@Min(1) @Max(3) Integer gamesToWin,
		@Min(1) @Max(50) Integer pointsToWin,
		@Min(1) @Max(50) Integer pointCap) {
}
