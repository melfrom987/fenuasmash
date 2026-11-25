package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(UUID id, UUID tournamentId, UUID eventId, Integer round, Integer bracketPosition,
		UUID sideA, UUID sideB, UUID courtId, MatchStatus status, MatchSide winner, MatchOutcome outcome,
		MatchScore score, int stateVersion, Instant startedAt, Instant endedAt, Instant createdAt, Instant updatedAt) {

	static MatchResponse from(Match match) {
		return new MatchResponse(match.getId(), match.getTournamentId(), match.getEventId(), match.getRound(),
				match.getBracketPosition(), match.getSideA(), match.getSideB(), match.getCourtId(), match.getStatus(),
				match.getWinner(), match.getOutcome(), match.getScore(), match.getStateVersion(), match.getStartedAt(),
				match.getEndedAt(), match.getCreatedAt(), match.getUpdatedAt());
	}

}
