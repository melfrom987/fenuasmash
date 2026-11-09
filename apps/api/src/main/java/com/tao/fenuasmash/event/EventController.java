package com.tao.fenuasmash.event;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class EventController {

	private final EventService service;

	EventController(EventService service) {
		this.service = service;
	}

	@GetMapping("/tournaments/{tournamentId}/events")
	List<EventResponse> findAllByTournament(@PathVariable UUID tournamentId) {
		return service.findAllByTournament(tournamentId);
	}

	@PostMapping("/tournaments/{tournamentId}/events")
	ResponseEntity<EventResponse> create(@PathVariable UUID tournamentId,
			@Valid @RequestBody CreateEventRequest request) {
		EventResponse created = service.create(tournamentId, request);
		return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
	}

	@GetMapping("/events/{id}")
	EventResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PatchMapping("/events/{id}")
	EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/events/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
