package com.tao.fenuasmash.match;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MatchRepository extends JpaRepository<Match, UUID> {

	/** By round, matches without a round last, then creation order. */
	@Query("SELECT m FROM Match m WHERE m.eventId = :eventId ORDER BY m.round ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByEvent(UUID eventId);

	@Query("SELECT m FROM Match m WHERE m.tournamentId = :tournamentId "
			+ "ORDER BY m.round ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByTournament(UUID tournamentId);

	@Query("SELECT m FROM Match m WHERE m.tournamentId = :tournamentId AND m.status IN :statuses "
			+ "ORDER BY m.round ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByTournamentAndStatusIn(UUID tournamentId, Collection<MatchStatus> statuses);

}
