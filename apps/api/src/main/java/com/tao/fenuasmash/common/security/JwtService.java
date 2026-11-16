package com.tao.fenuasmash.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Issues and verifies the access tokens: JWTs signed with HMAC-SHA256, holding
 * the account id ({@code sub}) and its organization id ({@code org}).
 */
@Component
public class JwtService {

	static final String ISSUER = "fenuasmash";

	static final String ORGANIZATION_CLAIM = "org";

	private static final int MIN_SECRET_BYTES = 32;

	private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

	private final JwtEncoder encoder;

	private final JwtDecoder decoder;

	private final Duration ttl;

	private final Clock clock;

	@Autowired
	JwtService(JwtProperties properties) {
		this(properties, Clock.systemUTC());
	}

	JwtService(JwtProperties properties, Clock clock) {
		byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"fenuasmash.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
		}
		SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
		// No clock skew: the API is the only one issuing and checking its tokens
		JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
		timestampValidator.setClock(clock);
		nimbusDecoder.setJwtValidator(
				new DelegatingOAuth2TokenValidator<>(timestampValidator, new JwtIssuerValidator(ISSUER)));
		this.decoder = nimbusDecoder;
		this.ttl = properties.ttl();
		this.clock = clock;
	}

	public String issue(UUID userId, UUID organizationId) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(userId.toString())
			.claim(ORGANIZATION_CLAIM, organizationId.toString())
			.issuedAt(now)
			.expiresAt(now.plus(ttl))
			.build();
		JwsHeader header = JwsHeader.with(ALGORITHM).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * Checks the signature, issuer and expiry of a token, then reads its claims.
	 * @throws InvalidTokenException if the token cannot be trusted
	 */
	public TokenClaims verify(String token) {
		Jwt jwt;
		try {
			jwt = decoder.decode(token);
		}
		catch (JwtException ex) {
			throw new InvalidTokenException("Invalid access token", ex);
		}
		if (jwt.getExpiresAt() == null) {
			throw new InvalidTokenException("Access token has no expiry");
		}
		return new TokenClaims(readUuid(jwt.getSubject()), readUuid(jwt.getClaimAsString(ORGANIZATION_CLAIM)));
	}

	private static UUID readUuid(String value) {
		if (value == null) {
			throw new InvalidTokenException("Access token is missing a claim");
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTokenException("Access token has a malformed claim", ex);
		}
	}

}
