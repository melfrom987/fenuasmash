package com.tao.fenuasmash.organization;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization as exposed by the API. Entities are never returned directly.
 */
public record OrganizationResponse(UUID id, String name, Instant createdAt) {

	static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(organization.getId(), organization.getName(), organization.getCreatedAt());
	}

}
