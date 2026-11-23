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
import com.tao.fenuasmash.account.UserRole;

/**
 * Issues and verifies the access tokens: JWTs signed with HMAC-SHA256, holding
 * the account id ({@code sub}), its role ({@code role}) and, for organizers,
 * its federation ({@code org}).
 */
@Component
public class JwtService {

	static final String ISSUER = "fenuasmash";

	static final String ROLE_CLAIM = "role";

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

	public IssuedToken issue(TokenClaims holder) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(ttl);
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(holder.userId().toString())
			.claim(ROLE_CLAIM, holder.role().name())
			.issuedAt(now)
			.expiresAt(expiresAt);
		if (holder.organizationId() != null) {
			claims.claim(ORGANIZATION_CLAIM, holder.organizationId().toString());
		}
		JwsHeader header = JwsHeader.with(ALGORITHM).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
		return new IssuedToken(value, expiresAt);
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
		UUID userId = readUuid(jwt.getSubject());
		UserRole role = readRole(jwt.getClaimAsString(ROLE_CLAIM));
		String organization = jwt.getClaimAsString(ORGANIZATION_CLAIM);
		UUID organizationId = organization != null ? readUuid(organization) : null;
		if ((role == UserRole.ORGANIZER) != (organizationId != null)) {
			throw new InvalidTokenException("Access token organization does not match its role");
		}
		return new TokenClaims(userId, role, organizationId);
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

	private static UserRole readRole(String value) {
		if (value == null) {
			throw new InvalidTokenException("Access token is missing a claim");
		}
		try {
			return UserRole.valueOf(value);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTokenException("Access token has an unknown role", ex);
		}
	}

}
