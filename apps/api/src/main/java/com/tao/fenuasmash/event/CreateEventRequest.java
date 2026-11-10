package com.tao.fenuasmash.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tao.fenuasmash.tournament.Discipline;

public record CreateEventRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull Discipline discipline,
		@NotBlank @Size(max = 100) String category,
		@Min(1) Integer poolCount,
		@Min(3) @Max(8) Integer poolSize,
		@Min(1) @Max(7) Integer qualifiersPerPool) {
}
