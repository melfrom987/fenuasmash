package com.tao.fenuasmash.tournament;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
class TournamentController {

	private final TournamentService service;

	TournamentController(TournamentService service) {
		this.service = service;
	}

	@GetMapping
	List<TournamentResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	TournamentResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping
	ResponseEntity<TournamentResponse> create(@Valid @RequestBody CreateTournamentRequest request) {
		TournamentResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/tournaments/" + created.id())).body(created);
	}

}
