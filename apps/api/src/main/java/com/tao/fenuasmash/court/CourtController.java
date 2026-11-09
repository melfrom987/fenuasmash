package com.tao.fenuasmash.court;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	@PostMapping("/tournaments/{tournamentId}/courts")
	ResponseEntity<CourtResponse> add(@PathVariable UUID tournamentId) {
		CourtResponse created = service.add(tournamentId);
		return ResponseEntity.created(URI.create("/api/courts/" + created.id())).body(created);
	}

	@GetMapping("/courts/{id}")
	CourtResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

}
