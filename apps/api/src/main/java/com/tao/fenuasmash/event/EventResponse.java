package com.tao.fenuasmash.event;

import java.util.UUID;

public record EventResponse(UUID id, UUID tournamentId, String name, EventType type) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getTournamentId(), event.getName(), event.getType());
	}

}
