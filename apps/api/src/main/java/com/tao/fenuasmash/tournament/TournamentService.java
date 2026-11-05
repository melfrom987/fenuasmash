package com.tao.fenuasmash.tournament;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		return repository.findById(id)
			.map(TournamentResponse::from)
			.orElseThrow(() -> new NotFoundException("Tournament", id));
	}

	@Transactional
	public TournamentResponse create(CreateTournamentRequest request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw new InvalidRequestException("INVALID_DATE_RANGE", "endsAt must be after startsAt");
		}
		if (!organizationService.exists(request.organizationId())) {
			throw new InvalidRequestException("INVALID_ORGANIZATION",
					"Organization " + request.organizationId() + " does not exist");
		}
		Tournament tournament = repository.save(new Tournament(request.organizationId(), request.name().strip(),
				request.startsAt(), request.endsAt(), request.minimumRestMinutes()));
		courtService.createInitialCourts(tournament.getId(), request.courtCount());
		return TournamentResponse.from(tournament);
	}

}
