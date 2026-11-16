package com.tao.fenuasmash.account;

import java.time.Instant;

/**
 * @param accessToken to send as {@code Authorization: Bearer <accessToken>}
 * @param tokenType always {@code Bearer}
 * @param expiresAt when the client has to log in again
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {
}
