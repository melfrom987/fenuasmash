package com.tao.fenuasmash.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Integration tests for {@code POST /api/accounts}, running against a real
 * PostgreSQL database started by Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountControllerTests {

	private static final String URL = "/api/accounts";

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

	private String adminToken;

	@BeforeEach
	void setUp() throws Exception {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Fédération Tahitienne de Badminton", Timestamp.from(Instant.now()));
		repository.saveAndFlush(
				new UserAccount("admin@example.com", passwordEncoder.encode(PASSWORD), UserRole.ADMIN, null));
		adminToken = login("admin@example.com", PASSWORD);
	}

	@Test
	void adminCreatesOrganizerWhoCanLogIn() throws Exception {
		create(adminToken, account("New.Orga@Example.com", "ORGANIZER", organizationId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.email").value("new.orga@example.com"))
			.andExpect(jsonPath("$.role").value("ORGANIZER"))
			.andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		assertThat(login("new.orga@example.com", PASSWORD)).isNotEmpty();
	}

	@Test
	void adminCreatesClubAccount() throws Exception {
		create(adminToken, account("club@example.com", "CLUB", null)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("CLUB"))
			.andExpect(jsonPath("$.organizationId").isEmpty());
	}

	@Test
	void passwordIsStoredHashed() throws Exception {
		create(adminToken, account("club@example.com", "CLUB", null)).andExpect(status().isCreated());

		String stored = repository.findByEmail("club@example.com").orElseThrow().getPasswordHash();
		assertThat(stored).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, stored)).isTrue();
	}

	@Test
	void creationWithoutTokenIsUnauthorized() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
			.content(account("club@example.com", "CLUB", null)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void creationByOtherRolesIsForbidden() throws Exception {
		repository.saveAndFlush(new UserAccount("orga@example.com", passwordEncoder.encode(PASSWORD),
				UserRole.ORGANIZER, organizationId));
		repository.saveAndFlush(
				new UserAccount("club@example.com", passwordEncoder.encode(PASSWORD), UserRole.CLUB, null));

		for (String email : new String[] { "orga@example.com", "club@example.com" }) {
			create(login(email, PASSWORD), account("other@example.com", "CLUB", null))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}
		assertThat(repository.findByEmail("other@example.com")).isEmpty();
	}

	@Test
	void adminAndPlayerAccountsCannotBeCreated() throws Exception {
		for (String role : new String[] { "ADMIN", "PLAYER" }) {
			create(adminToken, account("user@example.com", role, null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ROLE"));
		}
	}

	@Test
	void organizerWithoutOrganizationIsRejected() throws Exception {
		create(adminToken, account("orga@example.com", "ORGANIZER", null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("organizationId"));
	}

	@Test
	void clubWithOrganizationIsRejected() throws Exception {
		create(adminToken, account("club@example.com", "CLUB", organizationId)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("organizationId"));
	}

	@Test
	void unknownOrganizationIsRejected() throws Exception {
		create(adminToken, account("orga@example.com", "ORGANIZER", UUID.randomUUID()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ORGANIZATION"));
	}

	@Test
	void emailAlreadyUsedIsRejectedWhateverItsCase() throws Exception {
		create(adminToken, account("ADMIN@example.com", "CLUB", null)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
	}

	@Test
	void invalidFieldsAreRejected() throws Exception {
		create(adminToken, "{\"email\": \"not-an-email\", \"password\": \"short\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.length()").value(3));
	}

	@Test
	void passwordOver72BytesIsRejected() throws Exception {
		String json = "{\"email\": \"club@example.com\", \"password\": \"" + "é".repeat(40)
				+ "\", \"role\": \"CLUB\"}";

		create(adminToken, json).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("password"));
	}

	private static String account(String email, String role, UUID organization) {
		String organizationJson = organization == null ? "null" : "\"" + organization + "\"";
		return "{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\", \"role\": \"" + role
				+ "\", \"organizationId\": " + organizationJson + "}";
	}

	private ResultActions create(String token, String json) throws Exception {
		return mvc.perform(post(URL).header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private String login(String email, String password) throws Exception {
		String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
		String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

}
