package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface TabletRepository extends JpaRepository<Tablet, UUID> {

	Optional<Tablet> findByTokenHash(String tokenHash);

	List<Tablet> findAllByCourtIdIn(Collection<UUID> courtIds);

	/** Revokes the tablet paired with a court, if any. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Tablet t SET t.revokedAt = :now WHERE t.courtId = :courtId AND t.revokedAt IS NULL")
	int revokeActiveByCourtId(UUID courtId, Instant now);

}
