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
import com.tao.fenuasmash.court.CourtInitializer;
import com.tao.fenuasmash.organization.OrganizationService;

@Service
@Transactional(readOnly = true)
public class TournamentService {

	private final TournamentRepository repository;

	private final OrganizationService organizationService;

	private final CourtInitializer courtInitializer;

	TournamentService(TournamentRepository repository, OrganizationService organizationService,
			CourtInitializer courtInitializer) {
		this.repository = repository;
		this.organizationService = organizationService;
		this.courtInitializer = courtInitializer;
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
		ScoringRulesRequest scoring = request.scoring();
		TournamentRules rules = checkRules(new TournamentRules(request.competitionType(), request.drawFormat(),
				request.playMode(),
				new ScoringRules(scoring.gamesToWin(), scoring.pointsToWin(), scoring.twoPointLead(), scoring.pointCap()),
				request.relayStep(), request.disciplines(),
				Objects.requireNonNullElse(request.forbiddenCombinations(), List.of())));
		Tournament tournament = repository.save(new Tournament(request.organizationId(), request.name().strip(),
				request.startsAt(), request.endsAt(), request.minimumRestMinutes(), rules));
		courtInitializer.createInitialCourts(tournament.getId(), request.courtCount());
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
		TournamentRules rules = checkRules(mergeRules(tournament.getRules(), request));
		tournament.update(name, startsAt, endsAt, minimumRestMinutes, rules);
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

	private static TournamentRules mergeRules(TournamentRules current, UpdateTournamentRequest request) {
		PlayMode playMode = Objects.requireNonNullElse(request.playMode(), current.playMode());
		Integer relayStep = request.relayStep() != null ? request.relayStep()
				: playMode == PlayMode.RELAY ? current.relayStep() : null;
		return new TournamentRules(Objects.requireNonNullElse(request.competitionType(), current.competitionType()),
				Objects.requireNonNullElse(request.drawFormat(), current.drawFormat()), playMode,
				mergeScoring(current.scoring(), request.scoring()), relayStep,
				Objects.requireNonNullElse(request.disciplines(), current.disciplines()),
				Objects.requireNonNullElse(request.forbiddenCombinations(), current.forbiddenCombinations()));
	}

	private static ScoringRules mergeScoring(ScoringRules current, UpdateScoringRulesRequest requested) {
		if (requested == null) {
			return current;
		}
		boolean twoPointLead = Objects.requireNonNullElse(requested.twoPointLead(), current.twoPointLead());
		Integer pointCap = requested.pointCap() != null ? requested.pointCap()
				: twoPointLead ? current.pointCap() : null;
		return new ScoringRules(Objects.requireNonNullElse(requested.gamesToWin(), current.gamesToWin()),
				Objects.requireNonNullElse(requested.pointsToWin(), current.pointsToWin()), twoPointLead, pointCap);
	}

	private static TournamentRules checkRules(TournamentRules rules) {
		checkScoring(rules.scoring());
		checkRelay(rules);
		checkForbiddenCombinations(rules);
		return rules;
	}

	private static void checkForbiddenCombinations(TournamentRules rules) {
		for (List<Discipline> pair : rules.forbiddenCombinations()) {
			if (pair == null || pair.size() != 2 || pair.get(0) == null || pair.get(1) == null) {
				throw new InvalidRequestException("INVALID_FORBIDDEN_COMBINATION",
						"A forbidden combination must contain exactly two disciplines");
			}
			if (pair.get(0) == pair.get(1)) {
				throw new InvalidRequestException("INVALID_FORBIDDEN_COMBINATION",
						"A forbidden combination must contain two different disciplines");
			}
			if (!rules.disciplines().containsAll(pair)) {
				throw new InvalidRequestException("INVALID_FORBIDDEN_COMBINATION",
						"A forbidden combination must only contain disciplines offered by the tournament");
			}
		}
	}

	private static void checkScoring(ScoringRules scoring) {
		if (scoring.twoPointLead() && scoring.pointCap() == null) {
			throw new InvalidRequestException("INVALID_SCORING_RULES", "pointCap is required with a two-point lead");
		}
		if (!scoring.twoPointLead() && scoring.pointCap() != null) {
			throw new InvalidRequestException("INVALID_SCORING_RULES", "pointCap is only allowed with a two-point lead");
		}
		if (scoring.pointCap() != null && scoring.pointCap() < scoring.pointsToWin()) {
			throw new InvalidRequestException("INVALID_SCORING_RULES",
					"pointCap must be greater than or equal to pointsToWin");
		}
	}

	private static void checkRelay(TournamentRules rules) {
		if (rules.playMode() == PlayMode.SETS) {
			if (rules.relayStep() != null) {
				throw new InvalidRequestException("INVALID_RELAY_SETTINGS", "relayStep is only allowed in relay mode");
			}
			return;
		}
		if (rules.relayStep() == null) {
			throw new InvalidRequestException("INVALID_RELAY_SETTINGS", "relayStep is required in relay mode");
		}
	}

}
