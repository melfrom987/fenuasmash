package com.tao.fenuasmash.registration;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tao.fenuasmash.player.PlayerPage;

@RestController
@RequestMapping("/api")
class RegistrationController {

	private final RegistrationService service;

	RegistrationController(RegistrationService service) {
		this.service = service;
	}

	@GetMapping("/tournaments/{tournamentId}/registrations")
	List<RegistrationResponse> findAllByTournament(@PathVariable UUID tournamentId,
			@RequestParam(required = false) UUID eventId, @RequestParam(required = false) RegistrationStatus status,
			@RequestParam(required = false) UUID playerId) {
		return service.findAllByTournament(tournamentId, eventId, status, playerId);
	}

	@GetMapping("/events/{eventId}/registrations")
	List<RegistrationResponse> findAllByEvent(@PathVariable UUID eventId) {
		return service.findAllByEvent(eventId);
	}

	@PostMapping("/events/{eventId}/registrations")
	ResponseEntity<RegistrationResponse> create(@PathVariable UUID eventId,
			@Valid @RequestBody CreateRegistrationRequest request) {
		RegistrationResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/registrations/" + created.id())).body(created);
	}

	@PostMapping("/events/{eventId}/registrations/transfer")
	TransferResult transfer(@PathVariable UUID eventId, @Valid @RequestBody TransferRequest request) {
		return service.transfer(eventId, request);
	}

	@GetMapping("/events/{eventId}/eligible-players")
	PlayerPage findEligiblePlayers(@PathVariable UUID eventId, @RequestParam(required = false) String q,
			@RequestParam(required = false) UUID partnerOf, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.findEligiblePlayers(eventId, q, partnerOf, page, size);
	}

	@PatchMapping("/registrations/{id}")
	RegistrationResponse update(@PathVariable UUID id, @RequestBody UpdateRegistrationRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/registrations/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
