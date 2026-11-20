package com.tao.fenuasmash.common.security;

import static com.tao.fenuasmash.TestTokens.admin;
import static com.tao.fenuasmash.TestTokens.bearer;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.account.UserRole;

/**
 * Who may call which route. The ids are random: a request that passes the
 * access rules gets a 400 or 404 from the controller, one that does not gets a
 * 401 or 403 before reaching it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccessRulesTests {

	private static final UUID ID = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanDatabase() {
		DatabaseCleaner.clean(jdbc);
	}

	@Test
	void readsArePublic() throws Exception {
		for (String url : List.of("/api/organizations", "/api/tournaments", "/actuator/health")) {
			mvc.perform(get(url)).andExpect(status().isOk());
		}
		for (String url : List.of("/api/organizations/" + ID, "/api/tournaments/" + ID,
				"/api/tournaments/" + ID + "/events", "/api/tournaments/" + ID + "/courts", "/api/events/" + ID,
				"/api/courts/" + ID)) {
			mvc.perform(get(url)).andExpect(status().isNotFound());
		}
	}

	@Test
	void writesWithoutTokenAreUnauthorized() throws Exception {
		for (MockHttpServletRequestBuilder request : allWrites()) {
			mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		}
	}

	@Test
	void organizerManagesTournamentsEventsAndCourts() throws Exception {
		for (MockHttpServletRequestBuilder request : organizerWrites()) {
			mvc.perform(request.with(organizer(jwtService)))
				.andExpect(result -> assertThat(result.getResponse().getStatus())
					.as(result.getRequest().getMethod() + " " + result.getRequest().getRequestURI())
					.isIn(400, 404));
		}
	}

	@Test
	void otherRolesCannotManageTournamentsEventsAndCourts() throws Exception {
		for (RequestPostProcessor role : List.of(admin(jwtService), club(jwtService))) {
			for (MockHttpServletRequestBuilder request : organizerWrites()) {
				expectForbidden(request.with(role));
			}
		}
	}

	@Test
	void onlyAdminCreatesOrganizations() throws Exception {
		mvc.perform(createOrganization().with(admin(jwtService))).andExpect(status().isCreated());
		expectForbidden(createOrganization().with(organizer(jwtService)));
		expectForbidden(createOrganization().with(club(jwtService)));
	}

	@Test
	void onlyAdminCreatesAccounts() throws Exception {
		expectForbidden(json(post("/api/accounts"), "{}").with(organizer(jwtService)));
		expectForbidden(json(post("/api/accounts"), "{}").with(club(jwtService)));
	}

	@Test
	void unlistedRoutesAreClosed() throws Exception {
		mvc.perform(post("/api/unknown")).andExpect(status().isUnauthorized());
		expectForbidden(post("/api/unknown").with(organizer(jwtService)));
		expectForbidden(delete("/api/organizations/" + ID).with(admin(jwtService)));
		mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIsUnauthorized() throws Exception {
		Instant issuedAt = Instant.now().minus(jwtProperties.ttl()).minusSeconds(1);
		JwtService pastService = new JwtService(jwtProperties, Clock.fixed(issuedAt, ZoneOffset.UTC));
		RequestPostProcessor expired = bearer(pastService, organizerClaims());

		expectUnauthorized(json(post("/api/tournaments"), "{}").with(expired));
		expectUnauthorized(get("/api/tournaments").with(expired));
	}

	@Test
	void tokenSignedWithAnotherSecretIsUnauthorized() throws Exception {
		JwtService otherService = new JwtService(
				new JwtProperties("another-secret-that-is-long-enough-0123", jwtProperties.ttl()));
		RequestPostProcessor forged = bearer(otherService, organizerClaims());

		expectUnauthorized(json(post("/api/tournaments"), "{}").with(forged));
	}

	@Test
	void tokenWithRoleChangedIsUnauthorized() throws Exception {
		String token = jwtService.issue(new TokenClaims(UUID.randomUUID(), UserRole.CLUB, null)).value();
		String[] parts = token.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
			.replace("\"CLUB\"", "\"ADMIN\"");
		String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
			.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

		expectUnauthorized(json(post("/api/accounts"), "{}").header("Authorization", "Bearer " + forged));
	}

	@Test
	void playerRecordsAreReadOnlyByOrganizersAndClubs() throws Exception {
		for (String url : playerDataUrls()) {
			expectUnauthorized(get(url));
			expectForbidden(get(url).with(admin(jwtService)));
			mvc.perform(get(url).with(organizer(jwtService))).andExpect(status().isNotFound());
			mvc.perform(get(url).with(club(jwtService))).andExpect(status().isNotFound());
		}
	}

	@Test
	void onlyOrganizersManageClubsPlayersAndRegistrations() throws Exception {
		for (MockHttpServletRequestBuilder write : registrationWrites()) {
			expectUnauthorized(write);
		}
		for (RequestPostProcessor role : List.of(admin(jwtService), club(jwtService))) {
			for (MockHttpServletRequestBuilder write : registrationWrites()) {
				expectForbidden(write.with(role));
			}
		}
		for (MockHttpServletRequestBuilder write : registrationWrites()) {
			mvc.perform(write.with(organizer(jwtService))).andExpect(status().isNotFound());
		}
	}

	/** New builders on each call: a builder keeps the tokens added to it. */
	private static List<MockHttpServletRequestBuilder> registrationWrites() {
		return List.of(json(post("/api/clubs"), "{}"), json(patch("/api/clubs/" + ID), "{}"),
				json(post("/api/players"), "{}"), json(patch("/api/players/" + ID), "{}"),
				json(post("/api/events/" + ID + "/registrations"), "{}"), delete("/api/registrations/" + ID),
				json(post("/api/tournaments/" + ID + "/registrations/import"), "{}"));
	}

	@Test
	void clubsArePublicToRead() throws Exception {
		mvc.perform(get("/api/clubs")).andExpect(status().isNotFound());
	}

	private void expectUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
		mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private static TokenClaims organizerClaims() {
		return new TokenClaims(UUID.randomUUID(), UserRole.ORGANIZER, UUID.randomUUID());
	}

	private static List<String> playerDataUrls() {
		return List.of("/api/players", "/api/players/" + ID, "/api/registrations/" + ID,
				"/api/tournaments/" + ID + "/registrations", "/api/events/" + ID + "/registrations",
				"/api/events/" + ID + "/eligible-players");
	}

	private void expectForbidden(MockHttpServletRequestBuilder request) throws Exception {
		mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	private static List<MockHttpServletRequestBuilder> allWrites() {
		List<MockHttpServletRequestBuilder> writes = new ArrayList<>(organizerWrites());
		writes.add(createOrganization());
		writes.add(json(post("/api/accounts"), "{}"));
		return writes;
	}

	private static List<MockHttpServletRequestBuilder> organizerWrites() {
		return List.of(json(post("/api/tournaments"), "{}"), json(patch("/api/tournaments/" + ID), "{}"),
				delete("/api/tournaments/" + ID), json(post("/api/tournaments/" + ID + "/events"), "{}"),
				json(patch("/api/events/" + ID), "{}"), delete("/api/events/" + ID),
				post("/api/tournaments/" + ID + "/courts"), json(patch("/api/courts/" + ID), "{}"),
				delete("/api/courts/" + ID), post("/api/courts/" + ID + "/pairing-code"),
				post("/api/tablets/" + ID + "/revoke"), json(post("/api/events/" + ID + "/matches"), "{}"),
				json(post("/api/matches/" + ID + "/status"), "{}"));
	}

	private static MockHttpServletRequestBuilder createOrganization() {
		return json(post("/api/organizations"), "{\"name\": \"Fédération Tahitienne de Badminton\"}");
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
