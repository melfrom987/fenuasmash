package com.tao.fenuasmash.court;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the courts of a new tournament. Kept apart from {@link CourtService}, which
 * depends on the tournament feature, so that the tournament feature can call this class
 * without creating a dependency cycle.
 */
@Component
public class CourtInitializer {

	private final CourtRepository repository;

	CourtInitializer(CourtRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void createInitialCourts(UUID tournamentId, int count) {
		List<Court> courts = IntStream.rangeClosed(1, count).mapToObj(number -> new Court(tournamentId, number)).toList();
		repository.saveAll(courts);
	}

}
