package com.tao.fenuasmash.court;

import java.util.UUID;

public record CourtResponse(UUID id, UUID tournamentId, int number, boolean active) {

	static CourtResponse from(Court court) {
		return new CourtResponse(court.getId(), court.getTournamentId(), court.getNumber(), court.isActive());
	}

}
