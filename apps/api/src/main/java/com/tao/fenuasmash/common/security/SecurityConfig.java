package com.tao.fenuasmash.common.security;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security setup for a stateless JSON API: no session, no CSRF token, no login
 * form and no HTTP Basic. Clients authenticate with a bearer token.
 * <p>
 * Reads are public, except player records and registrations, which only
 * organizers and clubs can read. Organizers manage tournaments, events, courts,
 * matches, clubs, players, registrations and umpire tablets; the admin manages
 * accounts and organizations. A paired tablet reads its court, moves the match
 * on it and sends its score. Routes added later are closed until a rule lets them in.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProblemHandler problemHandler,
			JwtService jwtService, TabletAuthenticator tabletAuthenticator) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.exceptionHandling(errors -> errors.authenticationEntryPoint(problemHandler)
				.accessDeniedHandler(problemHandler))
			.addFilterBefore(new BearerTokenFilter(jwtService, tabletAuthenticator, problemHandler),
					UsernamePasswordAuthenticationFilter.class)
			// The first matching rule wins
			.authorizeHttpRequests(requests -> requests
				// Spring Boot's error page, reached after a request was already let in
				.dispatcherTypeMatchers(DispatcherType.ERROR)
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/tablets/pair")
				.permitAll()
				// Accounts only: a tablet has no account
				.requestMatchers(HttpMethod.GET, "/api/auth/me")
				.hasAnyRole("ADMIN", "ORGANIZER", "CLUB", "PLAYER")
				.requestMatchers(HttpMethod.GET, "/api/tablets/me", "/api/tablets/me/match")
				.hasRole(TabletPrincipal.ROLE)
				// The service checks that a tablet only acts on the match of its court
				.requestMatchers(HttpMethod.POST, "/api/matches/*/events")
				.hasRole(TabletPrincipal.ROLE)
				// Same check for status changes
				.requestMatchers(HttpMethod.POST, "/api/matches/*/status")
				.hasAnyRole("ORGANIZER", TabletPrincipal.ROLE)
				// When each tablet was last seen is for organizers only
				.requestMatchers(HttpMethod.GET, "/api/tournaments/*/tablets")
				.hasRole("ORGANIZER")
				.requestMatchers(HttpMethod.POST, "/api/tablets/*/revoke")
				.hasRole("ORGANIZER")
				.requestMatchers("/api/accounts", "/api/accounts/**")
				.hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/organizations")
				.hasRole("ADMIN")
				// Player records hold birth dates and contact details: only organizers and clubs read them
				.requestMatchers(HttpMethod.GET, "/api/players", "/api/players/**", "/api/registrations/**",
						"/api/tournaments/*/registrations", "/api/tournaments/*/registrations/**",
						"/api/events/*/registrations", "/api/events/*/eligible-players", "/api/events/*/draw")
				.hasAnyRole("ORGANIZER", "CLUB")
				// Tournaments, events, courts, clubs and organizations are public to read
				.requestMatchers(HttpMethod.GET, "/api/**")
				.permitAll()
				.requestMatchers("/api/tournaments", "/api/tournaments/**", "/api/events/**", "/api/courts/**",
						"/api/matches/**",
						"/api/clubs", "/api/clubs/**", "/api/players", "/api/players/**", "/api/registrations/**")
				.hasRole("ORGANIZER")
				// Anything not listed above is closed by default
				.anyRequest()
				.denyAll());
		return http.build();
	}

	/**
	 * Hashes passwords with bcrypt. The {@code {bcrypt}} prefix stored with each
	 * hash lets a stronger algorithm be adopted later without resetting them.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
