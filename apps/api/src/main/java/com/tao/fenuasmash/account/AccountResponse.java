package com.tao.fenuasmash.account;

import java.time.Instant;
import java.util.UUID;

/**
 * An account as seen by the admin. The password hash is never returned.
 */
public record AccountResponse(UUID id, String email, UserRole role, UUID organizationId, Instant createdAt) {

	static AccountResponse from(UserAccount account) {
		return new AccountResponse(account.getId(), account.getEmail(), account.getRole(),
				account.getOrganizationId(), account.getCreatedAt());
	}

}
