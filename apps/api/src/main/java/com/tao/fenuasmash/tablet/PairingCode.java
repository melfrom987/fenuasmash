package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The one-time code of a court, waiting to be typed on a tablet. A court has
 * one code at most; only its hash is stored.
 */
@Entity
@Table(name = "court_pairing_code")
class PairingCode {

	@Id
	@Column(name = "court_id")
	private UUID courtId;

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PairingCode() {
	}

	PairingCode(UUID courtId, String codeHash, Instant now, Instant expiresAt) {
		this.courtId = courtId;
		this.codeHash = codeHash;
		this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
		this.expiresAt = expiresAt.truncatedTo(ChronoUnit.MICROS);
	}

	boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	UUID getCourtId() {
		return courtId;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

}
