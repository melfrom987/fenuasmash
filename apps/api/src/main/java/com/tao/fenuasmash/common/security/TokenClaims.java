package com.tao.fenuasmash.common.security;

import java.util.UUID;

import com.tao.fenuasmash.account.UserRole;

/**
 * What a valid access token says about its holder.
 *
 * @param userId the account that logged in
 * @param role what the account is allowed to do
 * @param organizationId the federation of an organizer, null for other roles
 */
public record TokenClaims(UUID userId, UserRole role, UUID organizationId) {
}
