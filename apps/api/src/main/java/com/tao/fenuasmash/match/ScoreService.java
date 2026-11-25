package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.ForbiddenException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.common.security.TabletPrincipal;
import com.tao.fenuasmash.tournament.ScoringRules;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * Records the points of a match sent by the umpire tablet, and counts its
 * score.
 * <p>
 * The tablet may send the same events several times, late or out of order
 * after a network cut: an event already received is confirmed again without
 * being applied twice, and events are applied in sequence order, without gaps.
 * A batch is applied whole or not at all.
 * <p>
 * The tournament, then the match, are locked while a batch is applied: two
 * batches of one match are applied one after the other, and undoing the
 * winning point puts the match back on its court with the same checks as a
 * call to a court. Locks are always taken in this order, as in
 * {@link MatchService}, so that they cannot wait for each other.
 */
@Service
@Transactional(readOnly = true)
public class ScoreService {

	private final MatchRepository matchRepository;

	private final ScoreEventRepository eventRepository;

	private final MatchService matchService;

	private final TournamentService tournamentService;

	ScoreService(MatchRepository matchRepository, ScoreEventRepository eventRepository, MatchService matchService,
			TournamentService tournamentService) {
		this.matchRepository = matchRepository;
		this.eventRepository = eventRepository;
		this.matchService = matchService;
		this.tournamentService = tournamentService;
	}

	@Transactional
	public ScoreEventAck record(UUID matchId, ScoreEventBatchRequest batch, TabletPrincipal tablet) {
		UUID tournamentId = matchRepository.findTournamentId(matchId)
			.orElseThrow(() -> new NotFoundException("Match", matchId));
		matchRepository.lockTournament(tournamentId);
		Match match = matchRepository.findByIdForUpdate(matchId)
			.orElseThrow(() -> new NotFoundException("Match", matchId));
		if (!tablet.courtId().equals(match.getCourtId())) {
			throw new ForbiddenException("MATCH_NOT_ON_TABLET_COURT",
					"Match " + matchId + " is not on the court of tablet " + tablet.tabletId());
		}
		List<ScoreEventRequest> events = withoutRepeatedIds(batch.events());
		events.forEach(ScoreService::checkShape);

		// Events already received: confirmed again, never applied twice
		Map<UUID, ScoreEvent> known = eventRepository.findAllById(events.stream().map(ScoreEventRequest::id).toList())
			.stream()
			.collect(Collectors.toMap(ScoreEvent::getId, Function.identity()));
		List<UUID> duplicateIds = new ArrayList<>();
		List<ScoreEventRequest> fresh = new ArrayList<>();
		for (ScoreEventRequest event : events) {
			ScoreEvent previous = known.get(event.id());
			if (previous == null) {
				fresh.add(event);
			}
			else if (previous.isSameAs(matchId, event)) {
				duplicateIds.add(event.id());
			}
			else {
				throw new ConflictException("EVENT_ID_CONFLICT",
						"Event " + event.id() + " was already received with another content");
			}
		}

		List<ScoreEvent> recorded = eventRepository.findAllByMatchIdOrderBySequence(matchId);
		int lastSequence = recorded.isEmpty() ? 0 : recorded.getLast().getSequence();
		if (!fresh.isEmpty()) {
			fresh.sort(Comparator.comparingInt(ScoreEventRequest::sequence));
			lastSequence = apply(match, recorded, fresh, tablet);
		}
		return new ScoreEventAck(fresh.stream().map(ScoreEventRequest::id).toList(), duplicateIds, lastSequence,
				match.getScore(), match.getStatus(), match.getStateVersion());
	}

	/**
	 * Applies new events, in sequence order, after the recorded ones.
	 *
	 * @return the last sequence applied
	 */
	private int apply(Match match, List<ScoreEvent> recorded, List<ScoreEventRequest> fresh,
			TabletPrincipal tablet) {
		int expected = recorded.size() + 1;
		for (ScoreEventRequest event : fresh) {
			if (event.sequence() < expected) {
				throw new ConflictException("SEQUENCE_ALREADY_USED",
						"Sequence " + event.sequence() + " of match " + match.getId() + " is already used");
			}
			if (event.sequence() > expected) {
				throw new SequenceGapException(expected, event.sequence());
			}
			expected++;
		}

		ScoringRules rules = tournamentService.findById(match.getTournamentId()).scoring();
		List<MatchSide> rallies = new ArrayList<>();
		for (ScoreEvent event : recorded) {
			play(rallies, event.getType(), event.getSide());
		}
		ScoreSheet before = ScoreSheet.count(rallies, rules);
		boolean finishedOnPoints = match.getStatus() == MatchStatus.FINISHED
				&& match.getOutcome() == MatchOutcome.NORMAL && before.winner() != null;
		if (match.getStatus() != MatchStatus.IN_PROGRESS && !finishedOnPoints) {
			throw notInProgress(match);
		}

		ScoreSheet sheet = before;
		for (ScoreEventRequest event : fresh) {
			if (event.type() == ScoreEventType.POINT && sheet.winner() != null) {
				throw notInProgress(match);
			}
			if (event.type() == ScoreEventType.UNDO && rallies.isEmpty()) {
				throw new InvalidRequestException("INVALID_SCORE_EVENT",
						"Event " + event.id() + " undoes a point, but no point is left to undo");
			}
			play(rallies, event.type(), event.side());
			sheet = ScoreSheet.count(rallies, rules);
		}

		Instant now = Instant.now();
		eventRepository.saveAll(fresh.stream().map(event -> new ScoreEvent(match.getId(), event, tablet.tabletId(), now))
			.toList());
		match.updateScore(sheet, now);
		if (sheet.winner() != null && (match.getStatus() != MatchStatus.FINISHED || match.getWinner() != sheet.winner())) {
			match.moveTo(MatchStatus.FINISHED, sheet.winner(), MatchOutcome.NORMAL, now);
		}
		else if (sheet.winner() == null && match.getStatus() == MatchStatus.FINISHED) {
			// The winning point was undone: the match goes back on its court
			matchService.checkCourtIsFree(match, match.getCourtId());
			match.moveTo(MatchStatus.IN_PROGRESS, null, null, now);
		}
		// Flushed now, so that the answer carries the new state version
		matchRepository.saveAndFlush(match);
		return expected - 1;
	}

	private static void play(List<MatchSide> rallies, ScoreEventType type, MatchSide side) {
		if (type == ScoreEventType.POINT) {
			rallies.add(side);
		}
		else {
			rallies.removeLast();
		}
	}

	/** One copy of each event, when the tablet put the same one twice in a batch. */
	private static List<ScoreEventRequest> withoutRepeatedIds(List<ScoreEventRequest> events) {
		Map<UUID, ScoreEventRequest> byId = new LinkedHashMap<>();
		for (ScoreEventRequest event : events) {
			ScoreEventRequest other = byId.putIfAbsent(event.id(), event);
			if (other != null && !other.equals(event)) {
				throw new ConflictException("EVENT_ID_CONFLICT",
						"Event " + event.id() + " appears twice in the batch with different contents");
			}
		}
		return new ArrayList<>(byId.values());
	}

	private static void checkShape(ScoreEventRequest event) {
		if ((event.type() == ScoreEventType.POINT) != (event.side() != null)) {
			throw new InvalidRequestException("INVALID_SCORE_EVENT",
					"Event " + event.id() + ": a point needs the side that won the rally, an undo has none");
		}
	}

	private static ConflictException notInProgress(Match match) {
		return new ConflictException("MATCH_NOT_IN_PROGRESS",
				"Match " + match.getId() + " is " + match.getStatus() + ": it takes no new point");
	}

}
