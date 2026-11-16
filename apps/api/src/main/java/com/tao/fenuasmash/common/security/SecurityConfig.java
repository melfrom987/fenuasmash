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
 * Reads are public. Organizers manage tournaments, events and courts; the admin
 * manages accounts and organizations.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProblemHandler problemHandler,
			JwtService jwtService) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.exceptionHandling(errors -> errors.authenticationEntryPoint(problemHandler)
				.accessDeniedHandler(problemHandler))
			.addFilterBefore(new BearerTokenFilter(jwtService, problemHandler),
					UsernamePasswordAuthenticationFilter.class)
			// The first matching rule wins
			.authorizeHttpRequests(requests -> requests
				// Spring Boot's error page, reached after a request was already let in
				.dispatcherTypeMatchers(DispatcherType.ERROR)
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/me")
				.authenticated()
				.requestMatchers("/api/accounts", "/api/accounts/**")
				.hasRole("ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/organizations")
				.hasRole("ADMIN")
				// Tournaments, events, courts and organizations are public to read
				.requestMatchers(HttpMethod.GET, "/api/**")
				.permitAll()
				.requestMatchers("/api/tournaments", "/api/tournaments/**", "/api/events/**", "/api/courts/**")
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
