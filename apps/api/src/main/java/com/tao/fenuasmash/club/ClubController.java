package com.tao.fenuasmash.club;

import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/clubs")
class ClubController {

	private final ClubService service;

	ClubController(ClubService service) {
		this.service = service;
	}

	@GetMapping
	List<ClubResponse> findAll(@RequestParam(required = false) String q) {
		return service.findAll(q);
	}

	@PostMapping
	ResponseEntity<ClubResponse> create(@Valid @RequestBody ClubRequest request) {
		ClubResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/clubs/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	ClubResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PatchMapping("/{id}")
	ClubResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClubRequest request) {
		return service.update(id, request);
	}

}
