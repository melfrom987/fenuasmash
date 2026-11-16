package com.tao.fenuasmash.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import java.util.function.UnaryOperator;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Unit tests for {@link JwtService}, with a fixed clock so that expiry can be
 * checked to the second.
 */
class JwtServiceTests {

	private static final String SECRET = "test-secret-that-is-long-enough-0123456789";

	private static final Duration TTL = Duration.ofHours(12);

	private static final Instant NOW = Instant.parse("2026-11-14T08:00:00Z");

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID ORGANIZATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private final JwtService service = serviceAt(NOW);

	@Test
	void issuedTokenIsVerifiedWithItsClaims() {
		String token = service.issue(USER_ID, ORGANIZATION_ID);

		assertThat(service.verify(token)).isEqualTo(new TokenClaims(USER_ID, ORGANIZATION_ID));
	}

	@Test
	void tokenIsStillValidJustBeforeItsExpiry() {
		String token = service.issue(USER_ID, ORGANIZATION_ID);

		assertThat(serviceAt(NOW.plus(TTL).minusSeconds(1)).verify(token).userId()).isEqualTo(USER_ID);
	}

	@Test
	void expiredTokenIsRejected() {
		String token = service.issue(USER_ID, ORGANIZATION_ID);

		assertInvalid(serviceAt(NOW.plus(TTL).plusSeconds(1)), token);
	}

	@Test
	void tokenWithModifiedPayloadIsRejected() {
		String token = service.issue(USER_ID, ORGANIZATION_ID);
		String[] parts = token.split("\\.");
		String payload = decode(parts[1]).replace(ORGANIZATION_ID.toString(), UUID.randomUUID().toString());
		String forged = parts[0] + "." + encode(payload) + "." + parts[2];

		assertInvalid(service, forged);
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		String token = sign("another-secret-that-is-long-enough-0123", claims -> claims);

		assertInvalid(service, token);
	}

	@Test
	void unsignedTokenIsRejected() {
		String token = service.issue(USER_ID, ORGANIZATION_ID);
		String payload = token.split("\\.")[1];
		String unsigned = encode("{\"alg\":\"none\"}") + "." + payload + ".";

		assertInvalid(service, unsigned);
	}

	@Test
	void tokenFromAnotherIssuerIsRejected() {
		assertInvalid(service, sign(SECRET, claims -> claims.issuer("someone-else")));
	}

	@Test
	void tokenWithoutExpiryIsRejected() {
		assertInvalid(service, sign(SECRET, claims -> claims.claims(map -> map.remove("exp"))));
	}

	@Test
	void tokenWithoutOrganizationIsRejected() {
		assertInvalid(service, sign(SECRET, claims -> claims.claims(map -> map.remove(JwtService.ORGANIZATION_CLAIM))));
	}

	@Test
	void tokenWithMalformedSubjectIsRejected() {
		assertInvalid(service, sign(SECRET, claims -> claims.subject("not-a-uuid")));
	}

	@Test
	void malformedTokenIsRejected() {
		assertInvalid(service, "not-a-token");
		assertInvalid(service, "");
	}

	@Test
	void secretShorterThan32BytesIsRefused() {
		assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", TTL), Clock.systemUTC()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("at least 32 bytes");
	}

	private static JwtService serviceAt(Instant instant) {
		return new JwtService(new JwtProperties(SECRET, TTL), Clock.fixed(instant, ZoneOffset.UTC));
	}

	private static void assertInvalid(JwtService service, String token) {
		assertThatThrownBy(() -> service.verify(token)).isInstanceOf(InvalidTokenException.class);
	}

	/**
	 * Signs a token like {@link JwtService#issue} does, after letting the test
	 * change its claims.
	 */
	private static String sign(String secret, UnaryOperator<JwtClaimsSet.Builder> customizer) {
		var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(JwtService.ISSUER)
			.subject(USER_ID.toString())
			.claim(JwtService.ORGANIZATION_CLAIM, ORGANIZATION_ID.toString())
			.issuedAt(NOW)
			.expiresAt(NOW.plus(TTL));
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return new NimbusJwtEncoder(new ImmutableSecret<>(key))
			.encode(JwtEncoderParameters.from(header, customizer.apply(claims).build()))
			.getTokenValue();
	}

	private static String decode(String part) {
		return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
	}

	private static String encode(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

}
