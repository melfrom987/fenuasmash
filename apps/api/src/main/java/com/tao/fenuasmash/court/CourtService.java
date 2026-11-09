package com.tao.fenuasmash.court;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.tournament.TournamentService;

@Service
@Transactional(readOnly = true)
public class CourtService {

	private static final int MAX_COURTS = 50;

	private final CourtRepository repository;

	private final TournamentService tournamentService;

	CourtService(CourtRepository repository, TournamentService tournamentService) {
		this.repository = repository;
		this.tournamentService = tournamentService;
	}

	public List<CourtResponse> findAllByTournament(UUID tournamentId) {
		checkTournamentExists(tournamentId);
		return repository.findAllByTournamentIdOrderByNumber(tournamentId)
			.stream()
			.map(CourtResponse::from)
			.toList();
	}

	public CourtResponse findById(UUID id) {
		return CourtResponse.from(getCourt(id));
	}

	/**
	 * Rejects a court that cannot receive a new match because it has been deactivated.
	 */
	public void ensureActive(UUID id) {
		if (!getCourt(id).isActive()) {
			throw new ConflictException("COURT_INACTIVE", "Court " + id + " is inactive and cannot receive a new match");
		}
	}

	@Transactional
	public CourtResponse update(UUID id, UpdateCourtRequest request) {
		Court court = getCourt(id);
		court.update(request.active());
		return CourtResponse.from(court);
	}

	@Transactional
	public CourtResponse add(UUID tournamentId) {
		checkTournamentExists(tournamentId);
		List<Court> courts = repository.findAllByTournamentIdOrderByNumber(tournamentId);
		if (courts.size() >= MAX_COURTS) {
			throw new ConflictException("COURT_LIMIT_REACHED",
					"Tournament " + tournamentId + " already has the maximum of " + MAX_COURTS + " courts");
		}
		int number = courts.isEmpty() ? 1 : courts.getLast().getNumber() + 1;
		return CourtResponse.from(repository.save(new Court(tournamentId, number)));
	}

	@Transactional
	public void delete(UUID id) {
		Court court = getCourt(id);
		Court last = repository.findAllByTournamentIdOrderByNumber(court.getTournamentId()).getLast();
		if (last.getNumber() != court.getNumber()) {
			throw new ConflictException("COURT_NOT_LAST", "Only the last court of a tournament can be removed");
		}
		repository.delete(court);
	}

	private Court getCourt(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Court", id));
	}

	private void checkTournamentExists(UUID tournamentId) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
	}

}
