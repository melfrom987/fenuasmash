package com.tao.fenuasmash.registration;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ApiException;
import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.FieldValidationException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.draw.DrawLock;
import com.tao.fenuasmash.event.BirthYearBounds;
import com.tao.fenuasmash.event.EventResponse;
import com.tao.fenuasmash.event.EventService;
import com.tao.fenuasmash.player.EligiblePlayers;
import com.tao.fenuasmash.player.Gender;
import com.tao.fenuasmash.player.PlayerPage;
import com.tao.fenuasmash.player.PlayerService;
import com.tao.fenuasmash.player.PlayerSummary;
import com.tao.fenuasmash.tournament.Discipline;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * Registrations of players and pairs in events, with the rules of "Registrations" in
 * `docs/competition-rules.md`: gender against the discipline, birth-year bounds, maximum
 * entries, forbidden combinations, and a player at most once per event.
 */
@Service
@Transactional(readOnly = true)
public class RegistrationService {

	private static final Comparator<Registration> BY_DATE = Comparator.comparing(Registration::getRegisteredAt)
		.thenComparing(Registration::getCreatedAt)
		.thenComparing(Registration::getId);

	private final RegistrationRepository repository;

	private final EventService eventService;

	private final TournamentService tournamentService;

	private final PlayerService playerService;

	private final DrawLock drawLock;

	private final Clock clock;

	RegistrationService(RegistrationRepository repository, EventService eventService,
			TournamentService tournamentService, PlayerService playerService, DrawLock drawLock) {
		this.repository = repository;
		this.eventService = eventService;
		this.tournamentService = tournamentService;
		this.playerService = playerService;
		this.drawLock = drawLock;
		this.clock = Clock.systemUTC();
	}

	/** The registrations of an event, by registration date. */
	public List<RegistrationResponse> findAllByEvent(UUID eventId) {
		eventService.findById(eventId);
		return toResponses(repository.findByEventId(eventId).stream().sorted(BY_DATE).toList());
	}

	/** By event (in the order of the events of the tournament), then by registration date. */
	List<RegistrationResponse> findAllByTournament(UUID tournamentId, UUID eventId, RegistrationStatus status,
			UUID playerId) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
		Map<UUID, Integer> eventOrder = new HashMap<>();
		List<EventResponse> events = eventService.findAllByTournament(tournamentId);
		for (int i = 0; i < events.size(); i++) {
			eventOrder.put(events.get(i).id(), i);
		}
		List<Registration> registrations = repository.findByTournamentId(tournamentId)
			.stream()
			.filter((registration) -> eventId == null || registration.getEventId().equals(eventId))
			.filter((registration) -> status == null || registration.getStatus() == status)
			.filter((registration) -> playerId == null || registration.getPlayerId().equals(playerId)
					|| playerId.equals(registration.getPartnerId()))
			.sorted(Comparator
				.comparing((Registration registration) -> eventOrder.getOrDefault(registration.getEventId(),
						Integer.MAX_VALUE))
				.thenComparing(BY_DATE))
			.toList();
		return toResponses(registrations);
	}

	@Transactional
	RegistrationResponse create(UUID eventId, CreateRegistrationRequest request) {
		EventResponse event = eventService.findById(eventId);
		PlayerSummary player = getPlayer(request.playerId());
		PlayerSummary partner = null;
		if (request.partnerId() != null) {
			if (DisciplineRules.isSingles(event.discipline())) {
				throw new FieldValidationException("partnerId", "must be absent for singles");
			}
			if (request.partnerId().equals(request.playerId())) {
				throw new FieldValidationException("partnerId", "must be another player");
			}
			partner = getPlayer(request.partnerId());
		}
		checkPlayerEligible(event, player);
		if (partner != null) {
			checkPartnerEligible(event, player, partner);
		}
		checkNotRegistered(event, player, partner, null);
		checkNotFull(event);
		checkCombinations(event, player, null);
		if (partner != null) {
			checkCombinations(event, partner, null);
		}
		drawLock.beforeRegistrationsChange(eventId);
		RegistrationStatus status = (DisciplineRules.isSingles(event.discipline()) || partner != null)
				? RegistrationStatus.CONFIRMED : RegistrationStatus.LOOKING_FOR_PARTNER;
		Registration registration = repository.save(new Registration(event.tournamentId(), eventId, player.id(),
				(partner != null) ? partner.id() : null, status, clock.instant()));
		return toResponse(registration);
	}

	@Transactional
	RegistrationResponse update(UUID id, UpdateRegistrationRequest request) {
		Registration registration = getRegistration(id);
		EventResponse event = eventService.findById(registration.getEventId());
		drawLock.beforeRegistrationsChange(event.id());
		if (request.confirm()) {
			if (registration.getStatus() != RegistrationStatus.PENDING_PARTNER) {
				throw new ConflictException("NOT_PENDING", "Only a registration with a pending partner is confirmed");
			}
			registration.confirm();
		}
		if (request.partnerIdSent()) {
			UUID partnerId = request.partnerId();
			if (DisciplineRules.isSingles(event.discipline())) {
				if (partnerId != null) {
					throw new FieldValidationException("partnerId", "must be absent for singles");
				}
			}
			else if (partnerId == null) {
				registration.setPartner(null, RegistrationStatus.LOOKING_FOR_PARTNER);
			}
			else if (!partnerId.equals(registration.getPartnerId())) {
				if (partnerId.equals(registration.getPlayerId())) {
					throw new FieldValidationException("partnerId", "must be another player");
				}
				PlayerSummary player = getPlayer(registration.getPlayerId());
				PlayerSummary partner = getPlayer(partnerId);
				checkPartnerEligible(event, player, partner);
				checkNotRegistered(event, null, partner, registration.getId());
				checkCombinations(event, partner, registration.getId());
				registration.setPartner(partnerId, RegistrationStatus.CONFIRMED);
			}
			else {
				// The same partner, entered by the organizer: the pair is confirmed
				registration.confirm();
			}
		}
		return toResponse(registration);
	}

	@Transactional
	void delete(UUID id) {
		Registration registration = getRegistration(id);
		drawLock.beforeRegistrationsChange(registration.getEventId());
		repository.delete(registration);
	}

	/**
	 * Moves each registration into {@code eventId} on its own: those that do not fit the event
	 * are rejected with the reason, the others keep their status and registration date.
	 */
	@Transactional
	TransferResult transfer(UUID eventId, TransferRequest request) {
		EventResponse target = eventService.findById(eventId);
		drawLock.beforeRegistrationsChange(eventId);
		List<Registration> moved = new ArrayList<>();
		List<TransferResult.Rejection> rejected = new ArrayList<>();
		for (UUID registrationId : new LinkedHashSet<>(request.registrationIds())) {
			try {
				Registration registration = repository.findById(registrationId)
					.filter((found) -> found.getTournamentId().equals(target.tournamentId()))
					.orElseThrow(() -> new NotFoundException("Registration", registrationId));
				checkCompatible(registration, target);
				PlayerSummary player = getPlayer(registration.getPlayerId());
				PlayerSummary partner = (registration.getPartnerId() != null) ? getPlayer(registration.getPartnerId())
						: null;
				checkPlayerEligible(target, player);
				if (partner != null) {
					checkPartnerEligible(target, player, partner);
				}
				checkNotRegistered(target, player, partner, null);
				checkNotFull(target);
				drawLock.beforeRegistrationsChange(registration.getEventId());
				registration.moveTo(target.id());
				repository.saveAndFlush(registration);
				moved.add(registration);
			}
			catch (ApiException ex) {
				rejected.add(new TransferResult.Rejection(registrationId, ex.getCode(), ex.getMessage()));
			}
		}
		return new TransferResult(toResponses(moved), rejected);
	}

	/**
	 * One page of the players who can register in {@code eventId}, or partner
	 * {@code partnerOf} in it: right gender, right birth year, not registered in the event yet.
	 */
	PlayerPage findEligiblePlayers(UUID eventId, String query, UUID partnerOf, int page, int size) {
		EventResponse event = eventService.findById(eventId);
		Set<UUID> excluded = new HashSet<>();
		for (Registration registration : repository.findByEventId(eventId)) {
			excluded.add(registration.getPlayerId());
			if (registration.getPartnerId() != null) {
				excluded.add(registration.getPartnerId());
			}
		}
		Set<Gender> genders;
		if (partnerOf != null) {
			if (DisciplineRules.isSingles(event.discipline())) {
				throw new FieldValidationException("partnerOf", "must be absent for singles");
			}
			genders = DisciplineRules.partnerGenders(event.discipline(), getPlayer(partnerOf).gender());
			excluded.add(partnerOf);
		}
		else {
			genders = DisciplineRules.playerGenders(event.discipline());
		}
		BirthYearBounds years = birthYears(event);
		LocalDate bornFrom = (years.min() != null) ? LocalDate.of(years.min(), 1, 1) : null;
		LocalDate bornUntil = (years.max() != null) ? LocalDate.of(years.max(), 12, 31) : null;
		return playerService.searchEligible(query, new EligiblePlayers(genders, bornFrom, bornUntil, excluded), page,
				size);
	}

	/** Only between two events of the same discipline (the tournament is checked by the caller). */
	private void checkCompatible(Registration registration, EventResponse target) {
		if (registration.getEventId().equals(target.id())) {
			throw new InvalidRequestException("INCOMPATIBLE_EVENT", "The registration is already in this event");
		}
		Discipline source = eventService.findById(registration.getEventId()).discipline();
		if (source != target.discipline()) {
			throw new InvalidRequestException("INCOMPATIBLE_EVENT",
					"A " + source + " registration cannot move to a " + target.discipline() + " event");
		}
	}

	private void checkPlayerEligible(EventResponse event, PlayerSummary player) {
		if (!DisciplineRules.playerGenders(event.discipline()).contains(player.gender())) {
			throw notEligible("PLAYER_NOT_ELIGIBLE", player, "gender does not match " + event.discipline());
		}
		if (!birthYears(event).allows(player.birthDate())) {
			throw notEligible("PLAYER_NOT_ELIGIBLE", player, "birth year is outside the bounds of the event");
		}
	}

	private void checkPartnerEligible(EventResponse event, PlayerSummary player, PlayerSummary partner) {
		if (!DisciplineRules.partnerGenders(event.discipline(), player.gender()).contains(partner.gender())) {
			throw notEligible("PARTNER_NOT_ELIGIBLE", partner, "gender does not match " + event.discipline());
		}
		if (!birthYears(event).allows(partner.birthDate())) {
			throw notEligible("PARTNER_NOT_ELIGIBLE", partner, "birth year is outside the bounds of the event");
		}
	}

	/**
	 * Neither the player nor the partner may already be in the event, as the player or the
	 * partner of another registration than {@code except}.
	 */
	private void checkNotRegistered(EventResponse event, PlayerSummary player, PlayerSummary partner, UUID except) {
		List<UUID> ids = Stream.of(player, partner).filter(Objects::nonNull).map(PlayerSummary::id).toList();
		Set<UUID> registered = new HashSet<>();
		for (Registration registration : repository.findInEventWithAnyOf(event.id(), ids)) {
			if (!registration.getId().equals(except)) {
				registered.add(registration.getPlayerId());
				if (registration.getPartnerId() != null) {
					registered.add(registration.getPartnerId());
				}
			}
		}
		if (player != null && registered.contains(player.id())) {
			throw new ConflictException("ALREADY_REGISTERED", name(player) + " is already registered in this event");
		}
		if (partner != null && registered.contains(partner.id())) {
			throw new ConflictException("PARTNER_ALREADY_REGISTERED",
					name(partner) + " is already registered in this event");
		}
	}

	private void checkNotFull(EventResponse event) {
		if (event.maxEntries() != null && repository.countByEventId(event.id()) >= event.maxEntries()) {
			throw new ConflictException("EVENT_FULL", "The event already has " + event.maxEntries() + " entries");
		}
	}

	/**
	 * The player may not be registered in two events whose disciplines the tournament forbids
	 * together (e.g. singles and mixed doubles). {@code except} is left out.
	 */
	private void checkCombinations(EventResponse event, PlayerSummary player, UUID except) {
		List<List<Discipline>> forbidden = tournamentService.findById(event.tournamentId()).forbiddenCombinations();
		if (forbidden.isEmpty()) {
			return;
		}
		Map<UUID, Discipline> disciplines = new HashMap<>();
		for (EventResponse other : eventService.findAllByTournament(event.tournamentId())) {
			disciplines.put(other.id(), other.discipline());
		}
		for (Registration registration : repository.findInTournamentWith(event.tournamentId(), player.id())) {
			if (registration.getId().equals(except) || registration.getEventId().equals(event.id())) {
				continue;
			}
			Discipline other = disciplines.get(registration.getEventId());
			for (List<Discipline> pair : forbidden) {
				if ((pair.get(0) == event.discipline() && pair.get(1) == other)
						|| (pair.get(1) == event.discipline() && pair.get(0) == other)) {
					throw new InvalidRequestException("FORBIDDEN_COMBINATION", name(player)
							+ " cannot play both " + event.discipline() + " and " + other + " in this tournament");
				}
			}
		}
	}

	private static BirthYearBounds birthYears(EventResponse event) {
		return new BirthYearBounds(event.minBirthYear(), event.maxBirthYear());
	}

	private PlayerSummary getPlayer(UUID id) {
		return playerService.findSummary(id).orElseThrow(() -> new NotFoundException("Player", id));
	}

	private Registration getRegistration(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Registration", id));
	}

	private RegistrationResponse toResponse(Registration registration) {
		return toResponses(List.of(registration)).getFirst();
	}

	private List<RegistrationResponse> toResponses(List<Registration> registrations) {
		Set<UUID> playerIds = new HashSet<>();
		for (Registration registration : registrations) {
			playerIds.add(registration.getPlayerId());
			if (registration.getPartnerId() != null) {
				playerIds.add(registration.getPartnerId());
			}
		}
		Map<UUID, PlayerSummary> players = playerService.findSummaries(playerIds);
		return registrations.stream()
			.map((registration) -> new RegistrationResponse(registration.getId(), registration.getTournamentId(),
					registration.getEventId(), registration.getStatus(), players.get(registration.getPlayerId()),
					(registration.getPartnerId() != null) ? players.get(registration.getPartnerId()) : null,
					registration.getRegisteredAt(), registration.getCreatedAt()))
			.toList();
	}

	private static InvalidRequestException notEligible(String code, PlayerSummary player, String reason) {
		return new InvalidRequestException(code, name(player) + " cannot register in this event: " + reason);
	}

	private static String name(PlayerSummary player) {
		return player.firstName() + " " + player.lastName();
	}

}
