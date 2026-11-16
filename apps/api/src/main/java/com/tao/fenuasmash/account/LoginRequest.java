package com.tao.fenuasmash.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The email format is not checked here: a malformed email simply matches no
 * account, so it gets the same answer as a wrong password.
 */
public record LoginRequest(
		@NotBlank @Size(max = 254) String email,
		@NotBlank @Size(max = 128) String password) {
}
