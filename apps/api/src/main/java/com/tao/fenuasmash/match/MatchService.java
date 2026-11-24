package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.event.EventResponse;
import com.tao.fenuasmash.event.EventService;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * Creates matches and moves them through their statuses. Only the transitions
 * of {@link MatchStatus#nextStatuses()} are allowed, a forfeit or a finished
 * match always has a result, and a change based on an outdated
 * {@code stateVersion} is rejected.
 */
@Service
@Transactional(readOnly = true)
public class MatchService {

	static final String STATE_VERSION_CONFLICT = "STATE_VERSION_CONFLICT";

	private final MatchRepository repository;

	private final EventService eventService;

	private final TournamentService tournamentService;

	MatchService(MatchRepository repository, EventService eventService, TournamentService tournamentService) {
		this.repository = repository;
		this.eventService = eventService;
		this.tournamentService = tournamentService;
	}

	public List<MatchResponse> findAllByEvent(UUID eventId) {
		eventService.findById(eventId);
		return repository.findAllByEvent(eventId).stream().map(MatchResponse::from).toList();
	}

	/**
	 * @param statuses only the matches in these statuses; empty for all
	 */
	public List<MatchResponse> findAllByTournament(UUID tournamentId, Set<MatchStatus> statuses) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
		List<Match> matches = statuses.isEmpty() ? repository.findAllByTournament(tournamentId)
				: repository.findAllByTournamentAndStatusIn(tournamentId, statuses);
		return matches.stream().map(MatchResponse::from).toList();
	}

	public MatchResponse findById(UUID id) {
		return MatchResponse.from(getMatch(id));
	}

	@Transactional
	public MatchResponse create(UUID eventId, CreateMatchRequest request) {
		EventResponse event = eventService.findById(eventId);
		if (request.sideA() != null && request.sideA().equals(request.sideB())) {
			throw new InvalidRequestException("INVALID_MATCH_SIDES", "A registration cannot play against itself");
		}
		Match match = new Match(event.tournamentId(), eventId, request.round(), request.sideA(), request.sideB(),
				Instant.now());
		return MatchResponse.from(repository.saveAndFlush(match));
	}

	@Transactional
	public MatchResponse changeStatus(UUID id, ChangeMatchStatusRequest request) {
		Match match = getMatch(id);
		// First: a client working on an outdated match must read it again before anything else
		if (match.getStateVersion() != request.stateVersion()) {
			throw new ConflictException(STATE_VERSION_CONFLICT, "Match " + id + " is at state version "
					+ match.getStateVersion() + ", not " + request.stateVersion());
		}
		MatchStatus next = request.status();
		if (!match.getStatus().canMoveTo(next)) {
			throw new ConflictException("INVALID_STATUS_TRANSITION",
					"A match cannot go from " + match.getStatus() + " to " + next);
		}
		if (next.needsBothSides() && !match.hasBothSides()) {
			throw new ConflictException("MATCH_SIDES_INCOMPLETE", "Both sides of match " + id + " must be known");
		}
		MatchOutcome outcome = resolveOutcome(next, request.winner(), request.outcome());
		match.moveTo(next, request.winner(), outcome, Instant.now());
		// Flushed now, so that the answer carries the new state version
		return MatchResponse.from(repository.saveAndFlush(match));
	}

	/**
	 * Checks the result sent with a status: a winner and how it was won for a
	 * finished match, a winner for a forfeit, nothing otherwise.
	 */
	private static MatchOutcome resolveOutcome(MatchStatus next, MatchSide winner, MatchOutcome outcome) {
		return switch (next) {
			case FINISHED -> {
				if (winner == null || (outcome != MatchOutcome.NORMAL && outcome != MatchOutcome.RETIRED)) {
					throw invalidResult("A finished match needs a winner and an outcome, NORMAL or RETIRED");
				}
				yield outcome;
			}
			case WALKOVER -> {
				if (winner == null || (outcome != null && outcome != MatchOutcome.WALKOVER)) {
					throw invalidResult("A walkover needs the winner, and no other outcome");
				}
				yield MatchOutcome.WALKOVER;
			}
			default -> {
				if (winner != null || outcome != null) {
					throw invalidResult("Only a finished match or a walkover has a result");
				}
				yield null;
			}
		};
	}

	private static InvalidRequestException invalidResult(String message) {
		return new InvalidRequestException("INVALID_MATCH_RESULT", message);
	}

	private Match getMatch(UUID id) {
		return repository.findById(Objects.requireNonNull(id)).orElseThrow(() -> new NotFoundException("Match", id));
	}

}
