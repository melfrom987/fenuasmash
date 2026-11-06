package com.tao.fenuasmash.event;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.tournament.TournamentService;

@Service
@Transactional(readOnly = true)
public class EventService {

	private static final Comparator<Event> DISPLAY_ORDER = Comparator.comparing(Event::getType)
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
		checkTournamentExists(tournamentId);
		String name = request.name().strip();
		if (repository.existsByTournamentIdAndNameIgnoreCase(tournamentId, name)) {
			throw duplicateName(name);
		}
		return EventResponse.from(repository.save(new Event(tournamentId, name, request.type())));
	}

	@Transactional
	public EventResponse update(UUID id, UpdateEventRequest request) {
		Event event = getEvent(id);
		String name = request.name() != null ? request.name().strip() : event.getName();
		EventType type = Objects.requireNonNullElse(request.type(), event.getType());
		if (repository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(event.getTournamentId(), name, id)) {
			throw duplicateName(name);
		}
		event.update(name, type);
		return EventResponse.from(event);
	}

	@Transactional
	public void delete(UUID id) {
		repository.delete(getEvent(id));
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
