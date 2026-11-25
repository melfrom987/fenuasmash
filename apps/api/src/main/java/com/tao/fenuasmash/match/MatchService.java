package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.ForbiddenException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.common.security.TabletPrincipal;
import com.tao.fenuasmash.court.CourtResponse;
import com.tao.fenuasmash.court.CourtService;
import com.tao.fenuasmash.event.EventResponse;
import com.tao.fenuasmash.event.EventService;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * Creates matches and moves them through their statuses. Only the transitions
 * of {@link MatchStatus#nextStatuses()} are allowed, a forfeit or a finished
 * match always has a result, and a change based on an outdated
 * {@code stateVersion} is rejected.
 * <p>
 * Puts matches on courts: one match per court, and a side is never on two
 * courts. These checks run with the tournament locked, so that two calls made
 * at the same moment cannot break them.
 */
@Service
@Transactional(readOnly = true)
public class MatchService {

	static final String STATE_VERSION_CONFLICT = "STATE_VERSION_CONFLICT";

	private static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

	private final MatchRepository repository;

	private final EventService eventService;

	private final TournamentService tournamentService;

	private final CourtService courtService;

	private final ApplicationEventPublisher events;

	MatchService(MatchRepository repository, EventService eventService, TournamentService tournamentService,
			CourtService courtService, ApplicationEventPublisher events) {
		this.repository = repository;
		this.eventService = eventService;
		this.tournamentService = tournamentService;
		this.courtService = courtService;
		this.events = events;
	}

	public List<MatchResponse> findAllByEvent(UUID eventId) {
		eventService.findById(eventId);
		return repository.findAllByEvent(eventId).stream().map(MatchResponse::from).toList();
	}

	/**
	 * @param statuses only the matches in these statuses; empty for all
	 */
	public List<MatchResponse> findAllByTournament(UUID tournamentId, Set<MatchStatus> statuses) {
		checkTournamentExists(tournamentId);
		List<Match> matches = statuses.isEmpty() ? repository.findAllByTournament(tournamentId)
				: repository.findAllByTournamentAndStatusIn(tournamentId, statuses);
		return matches.stream().map(MatchResponse::from).toList();
	}

	public MatchResponse findById(UUID id) {
		return MatchResponse.from(getMatch(id));
	}

	/**
	 * The {@code READY} matches in calling order, each telling whether it can be
	 * called now: none of its sides is on a court.
	 */
	public List<QueuedMatchResponse> findQueue(UUID tournamentId) {
		checkTournamentExists(tournamentId);
		Set<UUID> sidesOnCourt = repository.findAllByTournamentAndStatusIn(tournamentId, MatchStatus.ON_COURT)
			.stream()
			.flatMap(match -> Stream.of(match.getSideA(), match.getSideB()))
			.collect(Collectors.toCollection(HashSet::new));
		return repository.findAllByTournamentAndStatusIn(tournamentId, Set.of(MatchStatus.READY))
			.stream()
			.map(match -> new QueuedMatchResponse(MatchResponse.from(match),
					!sidesOnCourt.contains(match.getSideA()) && !sidesOnCourt.contains(match.getSideB())))
			.toList();
	}

	/** Every court of the tournament, by number, with the match on it. */
	public List<CourtAssignmentResponse> findCourtAssignments(UUID tournamentId) {
		List<CourtResponse> courts = courtService.findAllByTournament(tournamentId);
		Map<UUID, Match> onCourt = repository.findAllByTournamentAndStatusIn(tournamentId, MatchStatus.ON_COURT)
			.stream()
			.collect(Collectors.toMap(Match::getCourtId, Function.identity()));
		return courts.stream().map(court -> {
			Match match = onCourt.get(court.id());
			return new CourtAssignmentResponse(court, match == null ? null : MatchResponse.from(match));
		}).toList();
	}

	/** The match on the court, if any. */
	public Optional<MatchResponse> findOnCourt(UUID courtId) {
		return repository.findOnCourt(courtId, MatchStatus.ON_COURT).map(MatchResponse::from);
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

	/**
	 * Creates the matches of the knockout bracket of an event, all {@code SCHEDULED}: those
	 * with both sides are ready to be played once the organizer marks them {@code READY}.
	 */
	@Transactional
	public List<MatchResponse> createKnockout(UUID eventId, List<KnockoutMatch> matches) {
		EventResponse event = eventService.findById(eventId);
		Instant now = Instant.now();
		List<Match> created = matches.stream()
			.map((match) -> new Match(event.tournamentId(), eventId, match.round(), match.position(), match.sideA(),
					match.sideB(), now))
			.toList();
		return repository.saveAllAndFlush(created).stream().map(MatchResponse::from).toList();
	}

	/**
	 * Calls a {@code READY} match to a court, or moves a {@code CALLED} match to
	 * another court.
	 */
	@Transactional
	public MatchResponse assignToCourt(UUID id, AssignMatchToCourtRequest request) {
		Match match = getMatch(id);
		checkStateVersion(match, request.stateVersion());
		MatchStatus current = match.getStatus();
		if (current != MatchStatus.READY && current != MatchStatus.CALLED) {
			throw new ConflictException(INVALID_STATUS_TRANSITION,
					"Only a READY or CALLED match can be called to a court, not a " + current + " one");
		}
		CourtResponse court = courtService.findById(request.courtId());
		if (!court.tournamentId().equals(match.getTournamentId())) {
			throw new InvalidRequestException("INVALID_COURT",
					"Court " + court.id() + " is not a court of the tournament of match " + id);
		}
		if (court.id().equals(match.getCourtId())) {
			return MatchResponse.from(match);
		}
		courtService.ensureActive(court.id());
		repository.lockTournament(match.getTournamentId());
		checkCourtIsFree(match, court.id());
		match.callTo(court.id(), Instant.now());
		return MatchResponse.from(repository.saveAndFlush(match));
	}

	/** A status change made by an organizer. */
	@Transactional
	public MatchResponse changeStatus(UUID id, ChangeMatchStatusRequest request) {
		return changeStatus(getMatch(id), request);
	}

	/**
	 * A status change made by the umpire tablet of a court: only on the match of
	 * its court, and only what {@link MatchStatus#tabletMayMoveTo} allows.
	 */
	@Transactional
	public MatchResponse changeStatus(UUID id, ChangeMatchStatusRequest request, TabletPrincipal tablet) {
		Match match = getMatch(id);
		boolean onTabletCourt = tablet.courtId().equals(match.getCourtId())
				&& (match.getStatus().isOnCourt() || match.getStatus() == MatchStatus.FINISHED);
		if (!onTabletCourt) {
			throw new ForbiddenException("MATCH_NOT_ON_TABLET_COURT",
					"Match " + id + " is not on the court of tablet " + tablet.tabletId());
		}
		if (!match.getStatus().tabletMayMoveTo(request.status())) {
			throw new ForbiddenException("STATUS_NOT_ALLOWED_FOR_TABLET", "A tablet cannot move a match from "
					+ match.getStatus() + " to " + request.status() + ": ask the organizer");
		}
		return changeStatus(match, request);
	}

	private MatchResponse changeStatus(Match match, ChangeMatchStatusRequest request) {
		UUID id = match.getId();
		// First: a client working on an outdated match must read it again before anything else
		checkStateVersion(match, request.stateVersion());
		MatchStatus current = match.getStatus();
		MatchStatus next = request.status();
		if (!current.canMoveTo(next)) {
			throw new ConflictException(INVALID_STATUS_TRANSITION, "A match cannot go from " + current + " to " + next);
		}
		if (next.needsBothSides() && !match.hasBothSides()) {
			throw new ConflictException("MATCH_SIDES_INCOMPLETE", "Both sides of match " + id + " must be known");
		}
		if (current == MatchStatus.READY && next == MatchStatus.CALLED) {
			throw courtRequired("A match is called with POST /api/matches/" + id + "/court, which sets its court");
		}
		MatchOutcome outcome = resolveOutcome(next, request.winner(), request.outcome());
		if (current == MatchStatus.FINISHED && next != MatchStatus.FINISHED) {
			checkCanReopen(match);
		}
		if (!current.isOnCourt() && next.isOnCourt()) {
			// Reopening a finished match puts it back on its court
			if (match.getCourtId() == null) {
				throw courtRequired("Match " + id + " has no court to be reopened on");
			}
			repository.lockTournament(match.getTournamentId());
			checkCourtIsFree(match, match.getCourtId());
		}
		UUID previousWinner = winnerOf(match);
		Instant now = Instant.now();
		match.moveTo(next, request.winner(), outcome, now);
		afterResultChange(match, previousWinner, now);
		// Flushed now, so that the answer carries the new state version
		return MatchResponse.from(repository.saveAndFlush(match));
	}

	/**
	 * The match the winner of a knockout match plays next: positions 2k-1 and 2k of a round
	 * feed position k of the next one. Empty for the final and for a match out of a bracket.
	 */
	private Optional<Match> followingMatch(Match match) {
		if (match.getBracketPosition() == null) {
			return Optional.empty();
		}
		return repository.findByEventIdAndRoundAndBracketPosition(match.getEventId(), match.getRound() + 1,
				(match.getBracketPosition() + 1) / 2);
	}

	/** The registration that won the match, or null while it has no winner. */
	static UUID winnerOf(Match match) {
		return (match.getStatus() == MatchStatus.FINISHED || match.getStatus() == MatchStatus.WALKOVER)
				? match.sideOf(match.getWinner()) : null;
	}

	/**
	 * A finished knockout match can be reopened only while its winner has not been called to
	 * the next match.
	 * @throws ConflictException `NEXT_MATCH_STARTED` otherwise
	 */
	void checkCanReopen(Match match) {
		Optional<Match> following = followingMatch(match);
		if (following.isPresent() && following.get().getStatus().compareTo(MatchStatus.READY) > 0) {
			throw new ConflictException("NEXT_MATCH_STARTED", "The winner of match " + match.getId()
					+ " already plays the next match: it cannot be reopened");
		}
	}

	/**
	 * After any change of status or score. A knockout match keeps its next match in line with
	 * its result: the winner takes its side (A from an odd position, B from an even one), and
	 * leaves it when the match is reopened; {@code previousWinner} is the winner before the
	 * change. A pool match tells that its pool may be over, or no longer ({@link PoolMatchChanged}).
	 */
	void afterResultChange(Match match, UUID previousWinner, Instant now) {
		followingMatch(match).ifPresent((nextMatch) -> moveWinnerOn(match, nextMatch, previousWinner, now));
		if (match.getPool() != null) {
			repository.flush();
			events.publishEvent(new PoolMatchChanged(match.getEventId(), match.getPool()));
		}
	}

	/** Creates the matches of the pools of an event, all {@code SCHEDULED}. */
	@Transactional
	public List<MatchResponse> createPoolMatches(UUID eventId, List<PoolMatch> matches) {
		EventResponse event = eventService.findById(eventId);
		Instant now = Instant.now();
		List<Match> created = matches.stream()
			.map((match) -> new Match(event.tournamentId(), eventId, match.round(), null, match.sideA(), match.sideB(),
					now)
				.inPool(match.pool()))
			.toList();
		return repository.saveAllAndFlush(created).stream().map(MatchResponse::from).toList();
	}

	/**
	 * Places a registration on a side of a knockout match not started yet, or empties it with
	 * null: a pool qualifier, once its pool is over.
	 * @throws ConflictException `NEXT_MATCH_STARTED` when the match is already called or played
	 */
	@Transactional
	public void placeInKnockout(UUID eventId, int round, int position, MatchSide side, UUID registration) {
		Match match = repository.findByEventIdAndRoundAndBracketPosition(eventId, round, position)
			.orElseThrow(() -> new IllegalStateException(
					"No knockout match at round " + round + ", position " + position + " of event " + eventId));
		if (Objects.equals(match.sideOf(side), registration)) {
			return;
		}
		if (match.getStatus().compareTo(MatchStatus.READY) > 0) {
			throw new ConflictException("NEXT_MATCH_STARTED",
					"The knockout match of this pool qualifier has already started");
		}
		match.placeOn(side, registration, Instant.now());
		repository.save(match);
	}

	private void moveWinnerOn(Match match, Match nextMatch, UUID previousWinner, Instant now) {
		MatchSide side = (match.getBracketPosition() % 2 == 1) ? MatchSide.A : MatchSide.B;
		UUID winner = winnerOf(match);
		UUID placed = nextMatch.sideOf(side);
		if (Objects.equals(placed, winner) || (winner == null && !Objects.equals(placed, previousWinner))) {
			return;
		}
		if (nextMatch.getStatus().compareTo(MatchStatus.READY) > 0) {
			throw new ConflictException("NEXT_MATCH_STARTED",
					"The next match of match " + match.getId() + " has already started");
		}
		nextMatch.placeOn(side, winner, now);
		repository.save(nextMatch);
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

	private static void checkStateVersion(Match match, int stateVersion) {
		if (match.getStateVersion() != stateVersion) {
			throw new ConflictException(STATE_VERSION_CONFLICT, "Match " + match.getId() + " is at state version "
					+ match.getStateVersion() + ", not " + stateVersion);
		}
	}

	/** To call with the tournament locked, so that the answer stays true until the commit. */
	void checkCourtIsFree(Match match, UUID courtId) {
		Optional<Match> onCourt = repository.findOnCourt(courtId, MatchStatus.ON_COURT)
			.filter(other -> !other.getId().equals(match.getId()));
		if (onCourt.isPresent()) {
			throw new ConflictException("COURT_OCCUPIED",
					"Court " + courtId + " already has match " + onCourt.get().getId());
		}
		if (repository.anySideOnCourt(match.getTournamentId(), match.getId(),
				List.of(match.getSideA(), match.getSideB()), MatchStatus.ON_COURT)) {
			throw new ConflictException("SIDE_ON_ANOTHER_COURT",
					"A side of match " + match.getId() + " is already on another court");
		}
	}

	private static ConflictException courtRequired(String message) {
		return new ConflictException("COURT_REQUIRED", message);
	}

	private void checkTournamentExists(UUID tournamentId) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
	}

	private static InvalidRequestException invalidResult(String message) {
		return new InvalidRequestException("INVALID_MATCH_RESULT", message);
	}

	private Match getMatch(UUID id) {
		return repository.findById(Objects.requireNonNull(id)).orElseThrow(() -> new NotFoundException("Match", id));
	}

}
