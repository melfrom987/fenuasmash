package com.tao.fenuasmash.organization;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A club, league or federation that organizes tournaments.
 */
@Entity
@Table(name = "organization")
public class Organization {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Organization() {
		// Required by JPA
	}

	public Organization(String name) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
