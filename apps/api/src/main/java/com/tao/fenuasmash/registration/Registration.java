package com.tao.fenuasmash.registration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration")
public class Registration {

	@Id
	private UUID id;

	@Column(name = "tournament_id", nullable = false, updatable = false)
	private UUID tournamentId;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "player_id", nullable = false, updatable = false)
	private UUID playerId;

	@Column(name = "partner_id")
	private UUID partnerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RegistrationStatus status;

	@Column(name = "registered_at", nullable = false)
	private Instant registeredAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Registration() {
	}

	Registration(UUID tournamentId, UUID eventId, UUID playerId, UUID partnerId, RegistrationStatus status,
			Instant now) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		this.eventId = eventId;
		this.playerId = playerId;
		this.partnerId = partnerId;
		this.status = status;
		this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
		this.registeredAt = this.createdAt;
	}

	void setPartner(UUID partnerId, RegistrationStatus status) {
		this.partnerId = partnerId;
		this.status = status;
	}

	void confirm() {
		this.status = RegistrationStatus.CONFIRMED;
	}

	/** Moves to another event, keeping the status and the registration date. */
	void moveTo(UUID eventId) {
		this.eventId = eventId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTournamentId() {
		return tournamentId;
	}

	public UUID getEventId() {
		return eventId;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public UUID getPartnerId() {
		return partnerId;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public Instant getRegisteredAt() {
		return registeredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
