package com.tao.fenuasmash.common.security;

import java.time.Instant;

/**
 * A freshly signed access token and the moment it stops being valid.
 */
public record IssuedToken(String value, Instant expiresAt) {
}
