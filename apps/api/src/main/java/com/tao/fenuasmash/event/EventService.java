package com.tao.fenuasmash.event;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.tournament.TournamentService;

@Service
@Transactional(readOnly = true)
public class EventService {

	private static final Comparator<Event> DISPLAY_ORDER = Comparator.comparing(Event::getType)
		.thenComparing(Event::getName, String.CASE_INSENSITIVE_ORDER);

	private static final EventSettings DEFAULT_SETTINGS = new EventSettings(EventFormat.SINGLE_ELIMINATION, null,
			null, ScoringRules.STANDARD);

	private final EventRepository repository;

	private final TournamentService tournamentService;

	EventService(EventRepository repository, TournamentService tournamentService) {
		this.repository = repository;
		this.tournamentService = tournamentService;
	}

	public List<EventResponse> findAllByTournament(UUID tournamentId) {
		checkTournamentExists(tournamentId);
		return repository.findByTournamentId(tournamentId)
			.stream()
			.sorted(DISPLAY_ORDER)
			.map(EventResponse::from)
			.toList();
	}

	public EventResponse findById(UUID id) {
		return EventResponse.from(getEvent(id));
	}

	@Transactional
	public EventResponse create(UUID tournamentId, CreateEventRequest request) {
		checkTournamentExists(tournamentId);
		String name = request.name().strip();
		if (repository.existsByTournamentIdAndNameIgnoreCase(tournamentId, name)) {
			throw duplicateName(name);
		}
		EventSettings settings = resolveSettings(DEFAULT_SETTINGS, request.format(), request.poolSize(),
				request.qualifiersPerPool(), request.scoring());
		return EventResponse.from(repository.save(new Event(tournamentId, name, request.type(), settings)));
	}

	@Transactional
	public EventResponse update(UUID id, UpdateEventRequest request) {
		Event event = getEvent(id);
		String name = request.name() != null ? request.name().strip() : event.getName();
		EventType type = Objects.requireNonNullElse(request.type(), event.getType());
		if (repository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(event.getTournamentId(), name, id)) {
			throw duplicateName(name);
		}
		EventSettings current = new EventSettings(event.getFormat(), event.getPoolSize(),
				event.getQualifiersPerPool(), event.getScoring());
		EventSettings settings = resolveSettings(current, request.format(), request.poolSize(),
				request.qualifiersPerPool(), request.scoring());
		event.update(name, type, settings);
		return EventResponse.from(event);
	}

	@Transactional
	public void delete(UUID id) {
		repository.delete(getEvent(id));
	}

	private static EventSettings resolveSettings(EventSettings current, EventFormat requestedFormat,
			Integer requestedPoolSize, Integer requestedQualifiersPerPool, ScoringRulesRequest requestedScoring) {
		EventFormat format = Objects.requireNonNullElse(requestedFormat, current.format());
		Integer poolSize = null;
		Integer qualifiersPerPool = null;
		if (format == EventFormat.POOLS_THEN_ELIMINATION) {
			boolean keepCurrent = current.format() == EventFormat.POOLS_THEN_ELIMINATION;
			poolSize = requestedPoolSize != null ? requestedPoolSize : keepCurrent ? current.poolSize() : null;
			qualifiersPerPool = requestedQualifiersPerPool != null ? requestedQualifiersPerPool
					: keepCurrent ? current.qualifiersPerPool() : null;
			if (poolSize == null || qualifiersPerPool == null) {
				throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
						"poolSize and qualifiersPerPool are required for format " + format);
			}
			if (qualifiersPerPool >= poolSize) {
				throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
						"qualifiersPerPool must be less than poolSize");
			}
		}
		else if (requestedPoolSize != null || requestedQualifiersPerPool != null) {
			throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
					"poolSize and qualifiersPerPool are only allowed for format "
							+ EventFormat.POOLS_THEN_ELIMINATION);
		}
		return new EventSettings(format, poolSize, qualifiersPerPool,
				resolveScoring(current.scoring(), requestedScoring));
	}

	private static ScoringRules resolveScoring(ScoringRules current, ScoringRulesRequest requested) {
		if (requested == null) {
			return current;
		}
		ScoringRules scoring = new ScoringRules(
				Objects.requireNonNullElse(requested.gamesToWin(), current.gamesToWin()),
				Objects.requireNonNullElse(requested.pointsToWin(), current.pointsToWin()),
				Objects.requireNonNullElse(requested.pointCap(), current.pointCap()));
		if (scoring.pointCap() < scoring.pointsToWin()) {
			throw new InvalidRequestException("INVALID_SCORING_RULES",
					"pointCap must be greater than or equal to pointsToWin");
		}
		return scoring;
	}

	private Event getEvent(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Event", id));
	}

	private void checkTournamentExists(UUID tournamentId) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
	}

	private static ConflictException duplicateName(String name) {
		return new ConflictException("EVENT_NAME_ALREADY_EXISTS",
				"An event named '" + name + "' already exists in this tournament");
	}

}
