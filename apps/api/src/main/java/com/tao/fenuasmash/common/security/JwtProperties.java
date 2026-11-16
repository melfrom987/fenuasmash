package com.tao.fenuasmash.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Settings of the access tokens, read from {@code fenuasmash.security.jwt.*}.
 *
 * @param secret HMAC key used to sign the tokens, at least 32 bytes
 * @param ttl how long a token stays valid after it is issued
 */
@Validated
@ConfigurationProperties("fenuasmash.security.jwt")
public record JwtProperties(@NotBlank String secret, @NotNull Duration ttl) {
}
