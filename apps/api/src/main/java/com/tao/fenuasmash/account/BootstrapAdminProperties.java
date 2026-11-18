package com.tao.fenuasmash.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The first admin account, read from {@code fenuasmash.bootstrap.admin.*}
 * (environment variables {@code FENUASMASH_BOOTSTRAP_ADMIN_EMAIL} and
 * {@code FENUASMASH_BOOTSTRAP_ADMIN_PASSWORD}). Both are optional: without them,
 * no account is created.
 */
@ConfigurationProperties("fenuasmash.bootstrap.admin")
public record BootstrapAdminProperties(String email, String password) {

	boolean isConfigured() {
		return hasText(email) || hasText(password);
	}

	boolean isComplete() {
		return hasText(email) && hasText(password);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
