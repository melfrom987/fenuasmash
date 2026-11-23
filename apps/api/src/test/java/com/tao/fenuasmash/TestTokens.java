package com.tao.fenuasmash;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.tao.fenuasmash.account.UserRole;
import com.tao.fenuasmash.common.security.JwtService;
import com.tao.fenuasmash.common.security.TokenClaims;

/**
 * Adds a real signed access token to a MockMvc request, without needing an
 * account in the database: {@code mvc.perform(post(url).with(organizer(jwt)))}.
 */
public final class TestTokens {

	private TestTokens() {
	}

	public static RequestPostProcessor organizer(JwtService jwtService) {
		return bearer(jwtService, new TokenClaims(UUID.randomUUID(), UserRole.ORGANIZER, UUID.randomUUID()));
	}

	public static RequestPostProcessor admin(JwtService jwtService) {
		return bearer(jwtService, new TokenClaims(UUID.randomUUID(), UserRole.ADMIN, null));
	}

	public static RequestPostProcessor club(JwtService jwtService) {
		return bearer(jwtService, new TokenClaims(UUID.randomUUID(), UserRole.CLUB, null));
	}

	public static RequestPostProcessor bearer(JwtService jwtService, TokenClaims claims) {
		String token = jwtService.issue(claims).value();
		return request -> {
			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
			return request;
		};
	}

}
