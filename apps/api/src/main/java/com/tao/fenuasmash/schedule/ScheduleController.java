package com.tao.fenuasmash.schedule;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The schedule of a tournament (tag `Schedule` in `contracts/openapi.yaml`). */
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/schedule")
class ScheduleController {

	private final ScheduleService service;

	ScheduleController(ScheduleService service) {
		this.service = service;
	}

	@GetMapping
	ScheduleResponse get(@PathVariable UUID tournamentId) {
		return service.get(tournamentId);
	}

	@PostMapping
	ScheduleResponse generate(@PathVariable UUID tournamentId,
			@Valid @RequestBody(required = false) ScheduleRequests.Generate request) {
		return service.generate(tournamentId, request);
	}

	@PostMapping("/move")
	ScheduleResponse move(@PathVariable UUID tournamentId, @Valid @RequestBody ScheduleRequests.Move request) {
		return service.move(tournamentId, request);
	}

	@PostMapping("/publish")
	ScheduleResponse publish(@PathVariable UUID tournamentId) {
		return service.publish(tournamentId);
	}

}
