package com.tao.fenuasmash.event;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tao.fenuasmash.tournament.Discipline;

public record CreateEventRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull Discipline discipline,
		@NotNull CategoryType categoryType,
		@Size(max = 50) String categoryLabel,
		Instant plannedDate,
		Instant plannedEndDate,
		@Min(2) @Max(256) Integer maxEntries,
		@Min(1900) @Max(2100) Integer minBirthYear,
		@Min(1900) @Max(2100) Integer maxBirthYear,
		@Min(1) Integer poolCount,
		@Min(3) @Max(8) Integer poolSize,
		@Min(1) @Max(7) Integer qualifiersPerPool) {
}
