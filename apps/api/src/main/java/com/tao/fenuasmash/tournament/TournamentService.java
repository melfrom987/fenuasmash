package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.court.CourtService;
import com.tao.fenuasmash.organization.OrganizationService;

@Service
@Transactional(readOnly = true)
public class TournamentService {

	private final TournamentRepository repository;

	private final OrganizationService organizationService;

	private final CourtService courtService;

	TournamentService(TournamentRepository repository, OrganizationService organizationService,
			CourtService courtService) {
		this.repository = repository;
		this.organizationService = organizationService;
		this.courtService = courtService;
	}

	public List<TournamentResponse> findAll() {
		return repository.findAll(Sort.by(Sort.Direction.DESC, "startsAt"))
			.stream()
			.map(TournamentResponse::from)
			.toList();
	}

	public TournamentResponse findById(UUID id) {
		return TournamentResponse.from(getTournament(id));
	}

	public boolean exists(UUID id) {
		return repository.existsById(id);
	}

	@Transactional
	public TournamentResponse create(CreateTournamentRequest request) {
		checkDateRange(request.startsAt(), request.endsAt());
		if (!organizationService.exists(request.organizationId())) {
			throw new InvalidRequestException("INVALID_ORGANIZATION",
					"Organization " + request.organizationId() + " does not exist");
		}
		Tournament tournament = repository.save(new Tournament(request.organizationId(), request.name().strip(),
				request.startsAt(), request.endsAt(), request.minimumRestMinutes()));
		courtService.createInitialCourts(tournament.getId(), request.courtCount());
		return TournamentResponse.from(tournament);
	}

	@Transactional
	public TournamentResponse update(UUID id, UpdateTournamentRequest request) {
		Tournament tournament = getTournament(id);
		String name = request.name() != null ? request.name().strip() : tournament.getName();
		Instant startsAt = Objects.requireNonNullElse(request.startsAt(), tournament.getStartsAt());
		Instant endsAt = Objects.requireNonNullElse(request.endsAt(), tournament.getEndsAt());
		int minimumRestMinutes = Objects.requireNonNullElse(request.minimumRestMinutes(),
				tournament.getMinimumRestMinutes());
		checkDateRange(startsAt, endsAt);
		tournament.update(name, startsAt, endsAt, minimumRestMinutes);
		return TournamentResponse.from(tournament);
	}

	@Transactional
	public void delete(UUID id) {
		Tournament tournament = getTournament(id);
		if (tournament.hasStarted(Instant.now())) {
			throw new ConflictException("TOURNAMENT_ALREADY_STARTED",
					"Tournament " + id + " has already started and cannot be deleted");
		}
		repository.delete(tournament);
	}

	private Tournament getTournament(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Tournament", id));
	}

	private static void checkDateRange(Instant startsAt, Instant endsAt) {
		if (!endsAt.isAfter(startsAt)) {
			throw new InvalidRequestException("INVALID_DATE_RANGE", "endsAt must be after startsAt");
		}
	}

}
