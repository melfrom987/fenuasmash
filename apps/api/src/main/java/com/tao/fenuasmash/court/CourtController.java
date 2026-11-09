package com.tao.fenuasmash.court;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CourtController {

	private final CourtService service;

	CourtController(CourtService service) {
		this.service = service;
	}

	@GetMapping("/tournaments/{tournamentId}/courts")
	List<CourtResponse> findAllByTournament(@PathVariable UUID tournamentId) {
		return service.findAllByTournament(tournamentId);
	}

}
