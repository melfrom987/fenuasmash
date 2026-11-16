package com.tao.fenuasmash.common.security;

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
 * form and no HTTP Basic. Clients will authenticate with a bearer token.
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
			// The other routes stay open until the access rules are added
			.authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.GET, "/api/auth/me")
				.authenticated()
				.anyRequest()
				.permitAll());
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
