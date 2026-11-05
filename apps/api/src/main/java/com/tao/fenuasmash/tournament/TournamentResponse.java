package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.util.UUID;

public record TournamentResponse(
		UUID id,
		UUID organizationId,
		String name,
		Instant startsAt,
		Instant endsAt,
		int minimumRestMinutes,
		Instant createdAt,
		Instant updatedAt) {

	static TournamentResponse from(Tournament tournament) {
		return new TournamentResponse(tournament.getId(), tournament.getOrganizationId(), tournament.getName(),
				tournament.getStartsAt(), tournament.getEndsAt(), tournament.getMinimumRestMinutes(),
				tournament.getCreatedAt(), tournament.getUpdatedAt());
	}

}
