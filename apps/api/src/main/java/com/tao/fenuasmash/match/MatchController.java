package com.tao.fenuasmash.match;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tao.fenuasmash.common.security.TabletPrincipal;

@RestController
@RequestMapping("/api")
class MatchController {

	private final MatchService service;

	private final ScoreService scoreService;

	MatchController(MatchService service, ScoreService scoreService) {
		this.service = service;
		this.scoreService = scoreService;
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

	@GetMapping("/tournaments/{tournamentId}/match-queue")
	List<QueuedMatchResponse> findQueue(@PathVariable UUID tournamentId) {
		return service.findQueue(tournamentId);
	}

	@GetMapping("/tournaments/{tournamentId}/court-assignments")
	List<CourtAssignmentResponse> findCourtAssignments(@PathVariable UUID tournamentId) {
		return service.findCourtAssignments(tournamentId);
	}

	@GetMapping("/tablets/me/match")
	ResponseEntity<MatchResponse> findTabletMatch(@AuthenticationPrincipal TabletPrincipal tablet) {
		return service.findOnCourt(tablet.courtId())
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/matches/{id}/events")
	ScoreEventAck recordScoreEvents(@PathVariable UUID id, @Valid @RequestBody ScoreEventBatchRequest request,
			@AuthenticationPrincipal TabletPrincipal tablet) {
		return scoreService.record(id, request, tablet);
	}

	@GetMapping("/matches/{id}")
	MatchResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping("/matches/{id}/court")
	MatchResponse assignToCourt(@PathVariable UUID id, @Valid @RequestBody AssignMatchToCourtRequest request) {
		return service.assignToCourt(id, request);
	}

	/** Organizers and the umpire tablet of the court of the match. */
	@PostMapping("/matches/{id}/status")
	MatchResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeMatchStatusRequest request,
			Authentication authentication) {
		if (authentication.getPrincipal() instanceof TabletPrincipal tablet) {
			return service.changeStatus(id, request, tablet);
		}
		return service.changeStatus(id, request);
	}

}
