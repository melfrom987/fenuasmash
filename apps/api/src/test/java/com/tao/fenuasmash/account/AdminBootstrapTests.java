package com.tao.fenuasmash.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

/**
 * Tests for the first admin account created at startup. The test context sets
 * no bootstrap settings, so each test builds its own {@link AdminBootstrap}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AdminBootstrapTests {

	private static final String PASSWORD = "correct horse battery staple";

	@Autowired
	private UserAccountRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanDatabase() {
		DatabaseCleaner.clean(jdbc);
	}

	@Test
	void createsTheFirstAdmin() {
		bootstrap(" Admin@Example.com ", PASSWORD).createAdminIfMissing();

		UserAccount admin = repository.findByEmail("admin@example.com").orElseThrow();
		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(admin.getOrganizationId()).isNull();
		assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
	}

	@Test
	void doesNothingWhenAnAdminAlreadyExists() {
		bootstrap("admin@example.com", PASSWORD).createAdminIfMissing();
		bootstrap("other.admin@example.com", "another long password").createAdminIfMissing();

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findByEmail("other.admin@example.com")).isEmpty();
	}

	@Test
	void doesNothingWhenNotConfigured() {
		bootstrap(null, null).createAdminIfMissing();
		bootstrap("", " ").createAdminIfMissing();

		assertThat(repository.count()).isZero();
	}

	@Test
	void refusesHalfConfiguration() {
		assertThatThrownBy(() -> bootstrap("admin@example.com", null).createAdminIfMissing())
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> bootstrap(null, PASSWORD).createAdminIfMissing())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void refusesShortPassword() {
		assertThatThrownBy(() -> bootstrap("admin@example.com", "short").createAdminIfMissing())
			.isInstanceOf(IllegalStateException.class);
		assertThat(repository.count()).isZero();
	}

	@Test
	void refusesEmailUsedByAnotherAccount() {
		repository.saveAndFlush(new UserAccount("club@example.com", "hash", UserRole.CLUB, null));

		assertThatThrownBy(() -> bootstrap("club@example.com", PASSWORD).createAdminIfMissing())
			.isInstanceOf(IllegalStateException.class);
	}

	private AdminBootstrap bootstrap(String email, String password) {
		return new AdminBootstrap(repository, passwordEncoder, new BootstrapAdminProperties(email, password));
	}

}
