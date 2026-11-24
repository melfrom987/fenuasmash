package com.tao.fenuasmash.match;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class MatchController {

	private final MatchService service;

	MatchController(MatchService service) {
		this.service = service;
	}

	@GetMapping("/events/{eventId}/matches")
	List<MatchResponse> findAllByEvent(@PathVariable UUID eventId) {
		return service.findAllByEvent(eventId);
	}

	@PostMapping("/events/{eventId}/matches")
	ResponseEntity<MatchResponse> create(@PathVariable UUID eventId, @Valid @RequestBody CreateMatchRequest request) {
		MatchResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/matches/" + created.id())).body(created);
	}

	@GetMapping("/tournaments/{tournamentId}/matches")
	List<MatchResponse> findAllByTournament(@PathVariable UUID tournamentId,
			@RequestParam(name = "status", required = false) Set<MatchStatus> statuses) {
		return service.findAllByTournament(tournamentId, statuses == null ? Set.of() : statuses);
	}

	@GetMapping("/matches/{id}")
	MatchResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping("/matches/{id}/status")
	MatchResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeMatchStatusRequest request) {
		return service.changeStatus(id, request);
	}

}
