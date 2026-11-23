package com.tao.fenuasmash.account;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An account that can log in. The password is only ever stored hashed.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

	@Id
	private UUID id;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	/** The federation of an organizer; null for every other role. */
	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UserAccount() {
		// Required by JPA
	}

	/**
	 * @param passwordHash the password already hashed by the password encoder
	 */
	public UserAccount(String email, String passwordHash, UserRole role, UUID organizationId) {
		Objects.requireNonNull(role, "role");
		if ((role == UserRole.ORGANIZER) != (organizationId != null)) {
			throw new IllegalArgumentException("An organization is required for organizers and only for them");
		}
		this.id = UUID.randomUUID();
		this.email = normalizeEmail(email);
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
		this.role = role;
		this.organizationId = organizationId;
		// PostgreSQL stores microseconds: truncate so the value never changes once persisted
		this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	/**
	 * Emails are compared without case or surrounding spaces, so they are stored
	 * that way.
	 */
	public static String normalizeEmail(String email) {
		return Objects.requireNonNull(email, "email").strip().toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
