package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An umpire tablet paired with a court. Only the hash of its token is stored.
 */
@Entity
@Table(name = "tablet")
class Tablet {

	@Id
	private UUID id;

	@Column(name = "court_id", nullable = false, updatable = false)
	private UUID courtId;

	@Column(name = "device_name", updatable = false)
	private String deviceName;

	@Column(name = "token_hash", nullable = false, updatable = false)
	private String tokenHash;

	@Column(name = "paired_at", nullable = false, updatable = false)
	private Instant pairedAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected Tablet() {
	}

	Tablet(UUID courtId, String deviceName, String tokenHash, Instant now) {
		this.id = UUID.randomUUID();
		this.courtId = courtId;
		this.deviceName = deviceName;
		this.tokenHash = tokenHash;
		this.pairedAt = now.truncatedTo(ChronoUnit.MICROS);
		this.lastSeenAt = this.pairedAt;
	}

	void recordActivity(Instant now) {
		this.lastSeenAt = now.truncatedTo(ChronoUnit.MICROS);
	}

	/** Keeps the first revocation date when the tablet is already revoked. */
	void revoke(Instant now) {
		if (revokedAt == null) {
			this.revokedAt = now.truncatedTo(ChronoUnit.MICROS);
		}
	}

	boolean isRevoked() {
		return revokedAt != null;
	}

	UUID getId() {
		return id;
	}

	UUID getCourtId() {
		return courtId;
	}

	String getDeviceName() {
		return deviceName;
	}

	Instant getPairedAt() {
		return pairedAt;
	}

	Instant getLastSeenAt() {
		return lastSeenAt;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}

}
