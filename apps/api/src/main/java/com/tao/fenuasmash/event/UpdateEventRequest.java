package com.tao.fenuasmash.event;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.tao.fenuasmash.tournament.Discipline;

public record UpdateEventRequest(
		@Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String name,
		Discipline discipline,
		CategoryType categoryType,
		@Size(max = 50) String categoryLabel,
		Instant plannedDate,
		@Min(2) @Max(256) Integer maxEntries,
		@Min(1) Integer poolCount,
		@Min(3) @Max(8) Integer poolSize,
		@Min(1) @Max(7) Integer qualifiersPerPool) {
}
