package com.tao.fenuasmash.account;

import java.util.UUID;

/**
 * @param organizationId the federation of an organizer, null for other roles
 */
public record CurrentUserResponse(UUID id, String email, UserRole role, UUID organizationId) {

	static CurrentUserResponse from(UserAccount account) {
		return new CurrentUserResponse(account.getId(), account.getEmail(), account.getRole(),
				account.getOrganizationId());
	}

}
