package com.tao.fenuasmash.event;

import java.time.Instant;
import java.util.UUID;

import com.tao.fenuasmash.tournament.Discipline;

public record EventResponse(
		UUID id,
		UUID tournamentId,
		String name,
		Discipline discipline,
		CategoryType categoryType,
		String categoryLabel,
		Instant plannedDate,
		Integer maxEntries,
		Integer poolCount,
		Integer poolSize,
		Integer qualifiersPerPool) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getTournamentId(), event.getName(), event.getDiscipline(),
				event.getCategoryType(), event.getCategoryLabel(), event.getPlannedDate(), event.getMaxEntries(),
				event.getPoolCount(), event.getPoolSize(), event.getQualifiersPerPool());
	}

}
