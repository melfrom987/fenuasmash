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
		Court court = repository.findById(id).orElseThrow(() -> new NotFoundException("Court", id));
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

	private void checkTournamentExists(UUID tournamentId) {
		if (!tournamentService.exists(tournamentId)) {
			throw new NotFoundException("Tournament", tournamentId);
		}
	}

}
