package com.tao.fenuasmash.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
		@Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String name,
		EventType type,
		EventFormat format,
		@Min(3) @Max(8) Integer poolSize,
		@Min(1) @Max(7) Integer qualifiersPerPool,
		@Valid ScoringRulesRequest scoring) {
}
