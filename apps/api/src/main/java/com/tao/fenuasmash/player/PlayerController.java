package com.tao.fenuasmash.player;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
class PlayerController {

	private final PlayerService service;

	PlayerController(PlayerService service) {
		this.service = service;
	}

	@GetMapping
	PlayerPage search(@RequestParam(required = false) String q, @RequestParam(required = false) UUID clubId,
			@RequestParam(required = false) Gender gender, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.search(q, clubId, gender, page, size);
	}

	@PostMapping
	ResponseEntity<PlayerResponse> create(@Valid @RequestBody CreatePlayerRequest request) {
		PlayerResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/players/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	PlayerResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PatchMapping("/{id}")
	PlayerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePlayerRequest request) {
		return service.update(id, request);
	}

}
