package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament")
public class Tournament {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Column(name = "minimum_rest_minutes", nullable = false)
	private int minimumRestMinutes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Tournament() {
	}

	Tournament(UUID organizationId, String name, Instant startsAt, Instant endsAt, int minimumRestMinutes) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
		this.name = name;
		this.startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
		this.endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
		this.minimumRestMinutes = minimumRestMinutes;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void update(String name, Instant startsAt, Instant endsAt, int minimumRestMinutes) {
		this.name = name;
		this.startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
		this.endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
		this.minimumRestMinutes = minimumRestMinutes;
		this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	boolean hasStarted(Instant now) {
		return !startsAt.isAfter(now);
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getName() {
		return name;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public int getMinimumRestMinutes() {
		return minimumRestMinutes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
