package com.tao.fenuasmash.account;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param organizationId required for an organizer, must be absent for a club
 */
public record CreateAccountRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 12, max = 72) String password,
		@NotNull UserRole role,
		UUID organizationId) {
}
