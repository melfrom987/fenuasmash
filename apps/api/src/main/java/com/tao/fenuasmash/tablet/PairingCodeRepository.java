package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PairingCodeRepository extends JpaRepository<PairingCode, UUID> {

	Optional<PairingCode> findByCodeHash(String codeHash);

	boolean existsByCodeHash(String codeHash);

	/**
	 * Uses a code. Returns 0 when another request used it first, so that a code
	 * pairs one tablet only.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM PairingCode c WHERE c.courtId = :courtId AND c.codeHash = :codeHash")
	int deleteByCourtIdAndCodeHash(UUID courtId, String codeHash);

	/** Removes the code of a court, so that a new one can replace it. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM PairingCode c WHERE c.courtId = :courtId")
	int deleteByCourtId(UUID courtId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM PairingCode c WHERE c.expiresAt <= :now")
	int deleteExpired(Instant now);

}
