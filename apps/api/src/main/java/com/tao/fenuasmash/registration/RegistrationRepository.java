package com.tao.fenuasmash.registration;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RegistrationRepository extends JpaRepository<Registration, UUID> {

	List<Registration> findByEventId(UUID eventId);

	List<Registration> findByTournamentId(UUID tournamentId);

	long countByEventId(UUID eventId);

	/** Registrations of {@code eventId} where one of {@code playerIds} is the player or the partner. */
	@Query("""
			select r from Registration r
			where r.eventId = :eventId and (r.playerId in :playerIds or r.partnerId in :playerIds)""")
	List<Registration> findInEventWithAnyOf(@Param("eventId") UUID eventId,
			@Param("playerIds") Collection<UUID> playerIds);

	/** Registrations of {@code tournamentId} where {@code playerId} is the player or the partner. */
	@Query("""
			select r from Registration r
			where r.tournamentId = :tournamentId and (r.playerId = :playerId or r.partnerId = :playerId)""")
	List<Registration> findInTournamentWith(@Param("tournamentId") UUID tournamentId,
			@Param("playerId") UUID playerId);

}
