package com.tao.fenuasmash.match;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MatchRepository extends JpaRepository<Match, UUID> {

	/** By round (matches without a round last), position in the bracket, then creation order. */
	@Query("SELECT m FROM Match m WHERE m.eventId = :eventId ORDER BY m.round ASC NULLS LAST, m.bracketPosition ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByEvent(UUID eventId);

	@Query("SELECT m FROM Match m WHERE m.tournamentId = :tournamentId "
			+ "ORDER BY m.round ASC NULLS LAST, m.bracketPosition ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByTournament(UUID tournamentId);

	@Query("SELECT m FROM Match m WHERE m.tournamentId = :tournamentId AND m.status IN :statuses "
			+ "ORDER BY m.round ASC NULLS LAST, m.bracketPosition ASC NULLS LAST, m.createdAt, m.id")
	List<Match> findAllByTournamentAndStatusIn(UUID tournamentId, Collection<MatchStatus> statuses);

	Optional<Match> findByEventIdAndRoundAndBracketPosition(UUID eventId, Integer round, Integer bracketPosition);

	@Query("SELECT m FROM Match m WHERE m.courtId = :courtId AND m.status IN :onCourt")
	Optional<Match> findOnCourt(UUID courtId, Collection<MatchStatus> onCourt);

	/** Whether a match other than {@code matchId} has one of these sides on a court. */
	@Query("SELECT COUNT(m) > 0 FROM Match m WHERE m.tournamentId = :tournamentId AND m.status IN :onCourt "
			+ "AND m.id <> :matchId AND (m.sideA IN :sides OR m.sideB IN :sides)")
	boolean anySideOnCourt(UUID tournamentId, UUID matchId, Collection<UUID> sides, Collection<MatchStatus> onCourt);

	/**
	 * Locks the tournament row until the end of the transaction, so that the
	 * matches of one tournament are put on courts one at a time.
	 */
	@Query(value = "SELECT CAST(id AS VARCHAR) FROM tournament WHERE id = :tournamentId FOR UPDATE",
			nativeQuery = true)
	String lockTournament(UUID tournamentId);

}
