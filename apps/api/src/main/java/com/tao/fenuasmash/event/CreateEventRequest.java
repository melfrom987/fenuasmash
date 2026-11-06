package com.tao.fenuasmash.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull EventType type,
		EventFormat format,
		@Min(3) @Max(8) Integer poolSize,
		@Min(1) @Max(7) Integer qualifiersPerPool,
		@Valid ScoringRulesRequest scoring) {
}
