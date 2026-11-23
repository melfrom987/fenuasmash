package com.tao.fenuasmash.account;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin account at startup, so that someone can log in and
 * create the other accounts. Does nothing once an admin exists, so the settings
 * can stay in place across restarts.
 */
@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private static final int MIN_PASSWORD_LENGTH = 12;

	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserAccountRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final BootstrapAdminProperties properties;

	AdminBootstrap(UserAccountRepository repository, PasswordEncoder passwordEncoder,
			BootstrapAdminProperties properties) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		createAdminIfMissing();
	}

	void createAdminIfMissing() {
		if (!properties.isConfigured()) {
			return;
		}
		if (!properties.isComplete()) {
			throw new IllegalStateException(
					"fenuasmash.bootstrap.admin.email and fenuasmash.bootstrap.admin.password must be set together");
		}
		if (repository.existsByRole(UserRole.ADMIN)) {
			return;
		}
		String password = properties.password();
		if (password.length() < MIN_PASSWORD_LENGTH
				|| password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new IllegalStateException("fenuasmash.bootstrap.admin.password must be " + MIN_PASSWORD_LENGTH
					+ " characters to " + MAX_PASSWORD_BYTES + " bytes long");
		}
		String email = UserAccount.normalizeEmail(properties.email());
		if (repository.existsByEmail(email)) {
			throw new IllegalStateException("fenuasmash.bootstrap.admin.email is already used by another account");
		}
		repository.save(new UserAccount(email, passwordEncoder.encode(password), UserRole.ADMIN, null));
		log.info("Created the first admin account {}", email);
	}

}
