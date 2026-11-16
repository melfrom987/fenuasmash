package com.tao.fenuasmash.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
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
import com.tao.fenuasmash.common.security.JwtService;
import com.tao.fenuasmash.common.security.TokenClaims;

/**
 * Integration tests for {@code POST /api/auth/login}, running against a real
 * PostgreSQL database started by Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

	private static final String URL = "/api/auth/login";

	private static final String PASSWORD = "correct horse battery staple";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserAccountRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

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
		organizer = repository.saveAndFlush(new UserAccount("orga@example.com", passwordEncoder.encode(PASSWORD),
				UserRole.ORGANIZER, organizationId));
	}

	@Test
	void loginReturnsBearerTokenForTheAccount() throws Exception {
		Instant before = Instant.now();

		String body = login("orga@example.com", PASSWORD).andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();

		TokenClaims claims = jwtService.verify(JsonPath.read(body, "$.accessToken"));
		assertThat(claims).isEqualTo(new TokenClaims(organizer.getId(), UserRole.ORGANIZER, organizationId));
		Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiresAt"));
		assertThat(expiresAt).isBetween(before.plus(Duration.ofHours(12)).minusSeconds(1),
				Instant.now().plus(Duration.ofHours(12)).plusSeconds(1));
	}

	@Test
	void loginIgnoresEmailCaseAndSpaces() throws Exception {
		login("  ORGA@Example.com ", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void loginWorksForRolesWithoutOrganization() throws Exception {
		UserAccount admin = repository.saveAndFlush(
				new UserAccount("admin@example.com", passwordEncoder.encode(PASSWORD), UserRole.ADMIN, null));

		String body = login("admin@example.com", PASSWORD).andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(jwtService.verify(JsonPath.read(body, "$.accessToken")))
			.isEqualTo(new TokenClaims(admin.getId(), UserRole.ADMIN, null));
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		expectInvalidCredentials(login("orga@example.com", "wrong password"));
	}

	@Test
	void unknownEmailGetsTheSameAnswerAsWrongPassword() throws Exception {
		String wrongPassword = expectInvalidCredentials(login("orga@example.com", "wrong password"));
		String unknownEmail = expectInvalidCredentials(login("nobody@example.com", PASSWORD));

		assertThat(unknownEmail).isEqualTo(wrongPassword);
	}

	@Test
	void passwordIsCaseSensitive() throws Exception {
		expectInvalidCredentials(login("orga@example.com", PASSWORD.toUpperCase()));
	}

	@Test
	void passwordLongerThanBcryptLimitIsRejected() throws Exception {
		expectInvalidCredentials(login("orga@example.com", PASSWORD + "x".repeat(128 - PASSWORD.length())));
	}

	@Test
	void blankFieldsAreRejected() throws Exception {
		login(" ", "").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.length()").value(2));
	}

	@Test
	void passwordOver128CharactersIsRejected() throws Exception {
		login("orga@example.com", "x".repeat(129)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("password"));
	}

	@Test
	void missingBodyIsRejected() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	private ResultActions login(String email, String password) throws Exception {
		String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
		return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/**
	 * @return the response body, to compare answers with each other
	 */
	private static String expectInvalidCredentials(ResultActions result) throws Exception {
		return result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value("Invalid email or password"))
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

}
