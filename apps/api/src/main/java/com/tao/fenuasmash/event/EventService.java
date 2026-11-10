package com.tao.fenuasmash.event;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.FieldValidationException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.tournament.Discipline;
import com.tao.fenuasmash.tournament.DrawFormat;
import com.tao.fenuasmash.tournament.TournamentResponse;
import com.tao.fenuasmash.tournament.TournamentService;

@Service
@Transactional(readOnly = true)
public class EventService {

	private static final Comparator<Event> DISPLAY_ORDER = Comparator.comparing(Event::getDiscipline)
		.thenComparing(Event::getName, String.CASE_INSENSITIVE_ORDER);

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
		TournamentResponse tournament = tournamentService.findById(tournamentId);
		String name = request.name().strip();
		String categoryLabel = resolveCategoryLabel(request.categoryType(), request.categoryLabel());
		if (repository.existsByTournamentIdAndNameIgnoreCase(tournamentId, name)) {
			throw duplicateName(name);
		}
		PoolStructure structure = resolveStructure(tournament.drawFormat(), PoolStructure.NONE, request.poolCount(),
				request.poolSize(), request.qualifiersPerPool());
		return EventResponse.from(repository
			.save(new Event(tournamentId, name, request.discipline(), request.categoryType(), categoryLabel, structure)));
	}

	@Transactional
	public EventResponse update(UUID id, UpdateEventRequest request) {
		Event event = getEvent(id);
		TournamentResponse tournament = tournamentService.findById(event.getTournamentId());
		String name = request.name() != null ? request.name().strip() : event.getName();
		CategoryType categoryType = Objects.requireNonNullElse(request.categoryType(), event.getCategoryType());
		String requestedLabel = request.categoryType() != null || request.categoryLabel() != null
				? request.categoryLabel() : event.getCategoryLabel();
		String categoryLabel = resolveCategoryLabel(categoryType, requestedLabel);
		Discipline discipline = Objects.requireNonNullElse(request.discipline(), event.getDiscipline());
		if (repository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(event.getTournamentId(), name, id)) {
			throw duplicateName(name);
		}
		PoolStructure current = new PoolStructure(event.getPoolCount(), event.getPoolSize(),
				event.getQualifiersPerPool());
		PoolStructure structure = resolveStructure(tournament.drawFormat(), current, request.poolCount(),
				request.poolSize(), request.qualifiersPerPool());
		event.update(name, discipline, categoryType, categoryLabel, structure);
		return EventResponse.from(event);
	}

	@Transactional
	public void delete(UUID id) {
		repository.delete(getEvent(id));
	}

	private static String resolveCategoryLabel(CategoryType categoryType, String categoryLabel) {
		if (categoryType == CategoryType.OPEN) {
			return null;
		}
		if (categoryLabel == null || categoryLabel.isBlank()) {
			throw new FieldValidationException("categoryLabel", "must not be blank");
		}
		return categoryLabel.strip();
	}

	private static PoolStructure resolveStructure(DrawFormat format, PoolStructure current, Integer requestedPoolCount,
			Integer requestedPoolSize, Integer requestedQualifiersPerPool) {
		if (format != DrawFormat.POOLS_THEN_ELIMINATION) {
			if (requestedPoolCount != null || requestedPoolSize != null || requestedQualifiersPerPool != null) {
				throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
						"Pool settings are only allowed when the tournament format is "
								+ DrawFormat.POOLS_THEN_ELIMINATION);
			}
			return PoolStructure.NONE;
		}
		Integer poolCount = requestedPoolCount != null ? requestedPoolCount : current.poolCount();
		Integer poolSize = requestedPoolSize != null ? requestedPoolSize : current.poolSize();
		Integer qualifiersPerPool = requestedQualifiersPerPool != null ? requestedQualifiersPerPool
				: current.qualifiersPerPool();
		if (poolCount == null && poolSize == null && qualifiersPerPool == null) {
			return PoolStructure.NONE;
		}
		if (poolCount == null || poolSize == null || qualifiersPerPool == null) {
			throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
					"poolCount, poolSize and qualifiersPerPool must be provided together");
		}
		if (qualifiersPerPool >= poolSize) {
			throw new InvalidRequestException("INVALID_FORMAT_SETTINGS",
					"qualifiersPerPool must be less than poolSize");
		}
		return new PoolStructure(poolCount, poolSize, qualifiersPerPool);
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
