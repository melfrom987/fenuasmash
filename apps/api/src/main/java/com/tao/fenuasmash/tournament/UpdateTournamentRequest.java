package com.tao.fenuasmash.tournament;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTournamentRequest(
		@Size(max = 200) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String name,
		Instant startsAt,
		Instant endsAt,
		@Min(0) @Max(1440) Integer minimumRestMinutes) {
}
