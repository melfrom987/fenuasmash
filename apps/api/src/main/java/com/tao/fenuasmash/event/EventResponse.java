package com.tao.fenuasmash.event;

import java.util.UUID;

import com.tao.fenuasmash.tournament.Discipline;

public record EventResponse(
		UUID id,
		UUID tournamentId,
		String name,
		Discipline discipline,
		String category,
		Integer poolCount,
		Integer poolSize,
		Integer qualifiersPerPool) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getTournamentId(), event.getName(), event.getDiscipline(),
				event.getCategory(), event.getPoolCount(), event.getPoolSize(), event.getQualifiersPerPool());
	}

}
