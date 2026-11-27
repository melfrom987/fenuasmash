package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(UUID id, UUID tournamentId, UUID eventId, Integer round, Integer bracketPosition,
		String pool, UUID sideA, UUID sideB, UUID courtId, MatchStatus status, MatchSide winner, MatchOutcome outcome,
		MatchScore score, int stateVersion, Instant startedAt, Instant endedAt, Instant createdAt, Instant updatedAt,
		Integer number, MatchPhase phase, MatchSource sourceA, MatchSource sourceB, Instant plannedStartAt,
		UUID plannedCourtId) {

	static MatchResponse from(Match match) {
		// A source is only shown while its side is not known
		return new MatchResponse(match.getId(), match.getTournamentId(), match.getEventId(), match.getRound(),
				match.getBracketPosition(), match.getPool(), match.getSideA(), match.getSideB(), match.getCourtId(), match.getStatus(),
				match.getWinner(), match.getOutcome(), match.getScore(), match.getStateVersion(), match.getStartedAt(),
				match.getEndedAt(), match.getCreatedAt(), match.getUpdatedAt(), match.getNumber(), match.getPhase(),
				(match.getSideA() == null) ? match.getSourceA() : null,
				(match.getSideB() == null) ? match.getSourceB() : null, match.getPlannedStartAt(),
				match.getPlannedCourtId());
	}

}
