package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTournamentRequest(
		@Size(max = 200) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String name,
		Instant startsAt,
		Instant endsAt,
		@Min(0) @Max(1440) Integer minimumRestMinutes,
		CompetitionType competitionType,
		DrawFormat drawFormat,
		PlayMode playMode,
		@Valid UpdateScoringRulesRequest scoring,
		@Min(1) @Max(50) Integer relayStep,
		@Size(min = 1) List<@NotNull Discipline> disciplines,
		List<List<Discipline>> forbiddenCombinations) {
}
