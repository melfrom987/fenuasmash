package com.tao.fenuasmash.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

/**
 * Integration tests for the {@code user_account} table, running against a real
 * PostgreSQL database started by Testcontainers.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserAccountRepositoryTests {

	@Autowired
	private UserAccountRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Fédération Tahitienne de Badminton", Timestamp.from(Instant.now()));
	}

	@Test
	void savedAccountIsFoundByItsNormalizedEmail() {
		UserAccount saved = repository.saveAndFlush(
				new UserAccount("  Mel@Example.COM ", passwordEncoder.encode("secret"), UserRole.ORGANIZER,
						organizationId));

		UserAccount found = repository.findByEmail(UserAccount.normalizeEmail("MEL@example.com")).orElseThrow();

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getEmail()).isEqualTo("mel@example.com");
		assertThat(found.getRole()).isEqualTo(UserRole.ORGANIZER);
		assertThat(found.getOrganizationId()).isEqualTo(organizationId);
		assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
	}

	@Test
	void passwordIsStoredHashed() {
		repository.saveAndFlush(new UserAccount("admin@example.com", passwordEncoder.encode("secret"),
				UserRole.ADMIN, null));

		String stored = jdbc.queryForObject("SELECT password_hash FROM user_account", String.class);

		assertThat(stored).startsWith("{bcrypt}").doesNotContain("secret");
		assertThat(passwordEncoder.matches("secret", stored)).isTrue();
		assertThat(passwordEncoder.matches("wrong", stored)).isFalse();
	}

	@Test
	void emailIsUniqueWhateverItsCase() {
		repository.saveAndFlush(new UserAccount("club@example.com", "hash", UserRole.CLUB, null));

		assertThatThrownBy(
				() -> repository.saveAndFlush(new UserAccount("CLUB@example.com", "hash", UserRole.PLAYER, null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unknownEmailIsNotFound() {
		assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void organizerWithoutOrganizationIsRefused() {
		assertThatThrownBy(() -> new UserAccount("orga@example.com", "hash", UserRole.ORGANIZER, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void otherRolesWithOrganizationAreRefused() {
		for (UserRole role : new UserRole[] { UserRole.ADMIN, UserRole.CLUB, UserRole.PLAYER }) {
			assertThatThrownBy(() -> new UserAccount("user@example.com", "hash", role, organizationId))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void databaseRefusesOrganizerWithoutOrganization() {
		assertThatThrownBy(() -> insert("orga@example.com", "ORGANIZER", null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRefusesUnknownRole() {
		assertThatThrownBy(() -> insert("user@example.com", "REFEREE", null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRefusesUppercaseEmail() {
		assertThatThrownBy(() -> insert("User@example.com", "ADMIN", null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRefusesUnknownOrganization() {
		assertThatThrownBy(() -> insert("orga@example.com", "ORGANIZER", UUID.randomUUID()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void insert(String email, String role, UUID organization) {
		jdbc.update(
				"INSERT INTO user_account (id, email, password_hash, role, organization_id, created_at) "
						+ "VALUES (?, ?, 'hash', ?, ?, ?)",
				UUID.randomUUID(), email, role, organization, Timestamp.from(Instant.now()));
	}

}
