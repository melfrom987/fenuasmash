package com.tao.fenuasmash.draw;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/draw")
class DrawController {

	private final DrawService service;

	DrawController(DrawService service) {
		this.service = service;
	}

	@GetMapping
	DrawResponse find(@PathVariable UUID eventId) {
		return service.find(eventId);
	}

	@PostMapping
	ResponseEntity<DrawResponse> create(@PathVariable UUID eventId, @Valid @RequestBody DrawSettingsRequest settings) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(eventId, settings));
	}

	@DeleteMapping
	ResponseEntity<Void> delete(@PathVariable UUID eventId) {
		service.delete(eventId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/swap")
	DrawResponse swap(@PathVariable UUID eventId, @Valid @RequestBody DrawSwapRequest request) {
		return service.swap(eventId, request);
	}

	@PostMapping("/publish")
	DrawResponse publish(@PathVariable UUID eventId) {
		return service.publish(eventId);
	}

}
