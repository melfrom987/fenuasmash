package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTournamentRequest(
		@NotNull UUID organizationId,
		@NotBlank @Size(max = 200) String name,
		@NotNull Instant startsAt,
		@NotNull Instant endsAt,
		@NotNull @Min(0) @Max(1440) Integer minimumRestMinutes,
		@NotNull @Min(1) @Max(50) Integer courtCount,
		@NotNull CompetitionType competitionType,
		@NotNull DrawFormat drawFormat,
		@NotNull PlayMode playMode,
		@NotNull @Valid ScoringRulesRequest scoring,
		@Min(1) @Max(50) Integer relayStep,
		@NotEmpty List<@NotNull Discipline> disciplines,
		List<List<Discipline>> forbiddenCombinations) {
}
