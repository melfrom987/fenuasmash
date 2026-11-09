package com.tao.fenuasmash.court;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourtService {

	private final CourtRepository repository;

	CourtService(CourtRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void createInitialCourts(UUID tournamentId, int count) {
		List<Court> courts = IntStream.rangeClosed(1, count).mapToObj(number -> new Court(tournamentId, number)).toList();
		repository.saveAll(courts);
	}

}
