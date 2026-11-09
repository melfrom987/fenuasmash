package com.tao.fenuasmash.event;

import java.util.UUID;

public record EventResponse(
		UUID id,
		UUID tournamentId,
		String name,
		EventType type,
		EventFormat format,
		Integer poolSize,
		Integer qualifiersPerPool,
		ScoringRules scoring) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getTournamentId(), event.getName(), event.getType(),
				event.getFormat(), event.getPoolSize(), event.getQualifiersPerPool(), event.getScoring());
	}

}
