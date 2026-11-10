package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TournamentResponse(
		UUID id,
		UUID organizationId,
		String name,
		Instant startsAt,
		Instant endsAt,
		int minimumRestMinutes,
		CompetitionType competitionType,
		DrawFormat drawFormat,
		PlayMode playMode,
		ScoringRules scoring,
		Integer relayStep,
		List<Discipline> disciplines,
		List<List<Discipline>> forbiddenCombinations,
		Instant createdAt,
		Instant updatedAt) {

	static TournamentResponse from(Tournament tournament) {
		TournamentRules rules = tournament.getRules();
		return new TournamentResponse(tournament.getId(), tournament.getOrganizationId(), tournament.getName(),
				tournament.getStartsAt(), tournament.getEndsAt(), tournament.getMinimumRestMinutes(),
				rules.competitionType(), rules.drawFormat(), rules.playMode(), rules.scoring(), rules.relayStep(),
				rules.disciplines(), rules.forbiddenCombinations(), tournament.getCreatedAt(),
				tournament.getUpdatedAt());
	}

}
