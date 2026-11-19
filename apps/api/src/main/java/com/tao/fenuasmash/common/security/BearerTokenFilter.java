package com.tao.fenuasmash.common.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads {@code Authorization: Bearer <token>} and, when the token is valid,
 * authenticates the request:
 * <ul>
 * <li>a JWT, issued at login, with its {@link TokenClaims} as principal and
 * {@code ROLE_<role>} as authority;</li>
 * <li>a tablet token (prefix {@code fst_}), checked in the database on every
 * request, with its {@link TabletPrincipal} as principal and
 * {@code ROLE_TABLET} as authority.</li>
 * </ul>
 * <p>
 * A request without a bearer token goes on anonymously and the access rules
 * decide. A request with an invalid or expired token is rejected with a 401,
 * even on a public route, so that the client knows it has to log in again.
 * <p>
 * Not a Spring bean on purpose: as a bean, Spring Boot would also register it
 * as a plain servlet filter and run it twice.
 */
class BearerTokenFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

	private static final String BEARER_PREFIX = "bearer ";

	private final JwtService jwtService;

	private final TabletAuthenticator tabletAuthenticator;

	private final AuthenticationEntryPoint entryPoint;

	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder
		.getContextHolderStrategy();

	BearerTokenFilter(JwtService jwtService, TabletAuthenticator tabletAuthenticator,
			AuthenticationEntryPoint entryPoint) {
		this.jwtService = jwtService;
		this.tabletAuthenticator = tabletAuthenticator;
		this.entryPoint = entryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		// The scheme is case-insensitive (RFC 9110); other schemes such as Basic are ignored
		if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			chain.doFilter(request, response);
			return;
		}
		String token = header.substring(BEARER_PREFIX.length()).strip();
		UsernamePasswordAuthenticationToken authentication;
		try {
			authentication = authenticate(token);
		}
		catch (InvalidTokenException ex) {
			log.debug("Rejected access token: {}", ex.getMessage());
			contextHolder.clearContext();
			entryPoint.commence(request, response, new BadCredentialsException("Invalid access token", ex));
			return;
		}
		SecurityContext context = contextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		contextHolder.setContext(context);
		chain.doFilter(request, response);
	}

	private UsernamePasswordAuthenticationToken authenticate(String token) {
		if (token.startsWith(TabletPrincipal.TOKEN_PREFIX)) {
			TabletPrincipal tablet = tabletAuthenticator.authenticate(token);
			return UsernamePasswordAuthenticationToken.authenticated(tablet, null,
					List.of(new SimpleGrantedAuthority("ROLE_" + TabletPrincipal.ROLE)));
		}
		TokenClaims claims = jwtService.verify(token);
		return UsernamePasswordAuthenticationToken.authenticated(claims, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name())));
	}

}
