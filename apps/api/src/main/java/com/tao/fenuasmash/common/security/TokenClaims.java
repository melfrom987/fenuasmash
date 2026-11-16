package com.tao.fenuasmash.common.security;

import java.util.UUID;

/**
 * What a valid access token says about its holder.
 *
 * @param userId the account that logged in
 * @param organizationId the organization this account belongs to
 */
public record TokenClaims(UUID userId, UUID organizationId) {
}
