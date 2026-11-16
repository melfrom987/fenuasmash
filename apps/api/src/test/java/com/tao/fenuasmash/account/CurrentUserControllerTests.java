package com.tao.fenuasmash.account;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

/**
 * Integration tests for {@code GET /api/auth/me} and for reading the bearer
 * token, running against a real PostgreSQL database started by Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CurrentUserControllerTests {

	private static final String URL = "/api/auth/me";

	private static final String PASSWORD = "correct horse battery staple";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserAccountRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	private UserAccount organizer;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Fédération Tahitienne de Badminton", Timestamp.from(Instant.now()));
		organizer = save("orga@example.com", UserRole.ORGANIZER, organizationId);
	}

	@Test
	void meReturnsTheLoggedInAccount() throws Exception {
		me("Bearer " + login("orga@example.com")).andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.id").value(organizer.getId().toString()))
			.andExpect(jsonPath("$.email").value("orga@example.com"))
			.andExpect(jsonPath("$.role").value("ORGANIZER"))
			.andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void meReturnsNullOrganizationForOtherRoles() throws Exception {
		save("club@example.com", UserRole.CLUB, null);

		me("Bearer " + login("club@example.com")).andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("CLUB"))
			.andExpect(jsonPath("$.organizationId").isEmpty());
	}

	@Test
	void bearerSchemeIsCaseInsensitive() throws Exception {
		me("bearer " + login("orga@example.com")).andExpect(status().isOk());
	}

	@Test
	void meWithoutTokenIsUnauthorized() throws Exception {
		expectUnauthorized(mvc.perform(get(URL)));
	}

	@Test
	void meWithOtherSchemeIsUnauthorized() throws Exception {
		expectUnauthorized(me("Basic dXNlcjpwYXNzd29yZA=="));
	}

	@Test
	void meWithMalformedTokenIsUnauthorized() throws Exception {
		expectUnauthorized(me("Bearer not-a-token"));
		expectUnauthorized(me("Bearer "));
	}

	@Test
	void meWithModifiedTokenIsUnauthorized() throws Exception {
		String token = login("orga@example.com");
		String[] parts = token.split("\\.");
		String forged = parts[0] + "." + parts[1] + "." + new StringBuilder(parts[2]).reverse();

		expectUnauthorized(me("Bearer " + forged));
	}

	@Test
	void meForDeletedAccountIsUnauthorized() throws Exception {
		String token = login("orga@example.com");
		repository.deleteAll();

		me("Bearer " + token).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void publicRouteStillWorksWithoutToken() throws Exception {
		mvc.perform(get("/api/organizations")).andExpect(status().isOk());
	}

	@Test
	void publicRouteWorksWithValidToken() throws Exception {
		mvc.perform(get("/api/organizations").header("Authorization", "Bearer " + login("orga@example.com")))
			.andExpect(status().isOk());
	}

	@Test
	void publicRouteWithInvalidTokenIsUnauthorized() throws Exception {
		expectUnauthorized(mvc.perform(get("/api/organizations").header("Authorization", "Bearer not-a-token")));
	}

	private UserAccount save(String email, UserRole role, UUID organization) {
		return repository.saveAndFlush(new UserAccount(email, passwordEncoder.encode(PASSWORD), role, organization));
	}

	private String login(String email) throws Exception {
		String json = "{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}";
		String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions me(String authorization) throws Exception {
		return mvc.perform(get(URL).header("Authorization", authorization));
	}

	private static void expectUnauthorized(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
			.andExpect(header().string("WWW-Authenticate", "Bearer"))
			.andExpect(header().string("Content-Type", startsWith("application/problem+json")))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

}
