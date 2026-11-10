package com.tao.fenuasmash.event;

import java.util.UUID;

import com.tao.fenuasmash.tournament.Discipline;

public record EventResponse(
		UUID id,
		UUID tournamentId,
		String name,
		Discipline discipline,
		CategoryType categoryType,
		String categoryLabel,
		Integer poolCount,
		Integer poolSize,
		Integer qualifiersPerPool) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getTournamentId(), event.getName(), event.getDiscipline(),
				event.getCategoryType(), event.getCategoryLabel(), event.getPoolCount(), event.getPoolSize(),
				event.getQualifiersPerPool());
	}

}
