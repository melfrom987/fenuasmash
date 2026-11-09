package com.tao.fenuasmash.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TournamentControllerTests {

	private static final String URL = "/api/tournaments";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
	}

	@Test
	void createReturnsCreatedTournamentWithLocation() throws Exception {
		create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 4))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith(URL + "/")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
			.andExpect(jsonPath("$.name").value("Open de Papeete"))
			.andExpect(jsonPath("$.startsAt").value("2026-11-14T18:00:00Z"))
			.andExpect(jsonPath("$.endsAt").value("2026-11-16T04:00:00Z"))
			.andExpect(jsonPath("$.minimumRestMinutes").value(20))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.updatedAt").isNotEmpty());
	}

	@Test
	void createAddsNumberedActiveCourts() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 3));

		List<Integer> numbers = jdbc.queryForList(
				"SELECT number FROM court WHERE tournament_id = ? AND active ORDER BY number", Integer.class,
				tournamentId);
		assertThat(numbers).containsExactly(1, 2, 3);
	}

	@Test
	void createTrimsName() throws Exception {
		create(body("  Open de Papeete  ", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 1))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Open de Papeete"));
	}

	@Test
	void createRejectsUnknownOrganization() throws Exception {
		organizationId = UUID.randomUUID();

		assertBadRequest(create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 4)),
				"INVALID_ORGANIZATION");
		assertNoTournamentCreated();
	}

	@Test
	void createRejectsEndBeforeStart() throws Exception {
		assertBadRequest(create(body("Open de Papeete", "2026-11-16T04:00:00Z", "2026-11-14T18:00:00Z", 20, 4)),
				"INVALID_DATE_RANGE");
	}

	@Test
	void createRejectsEndEqualToStart() throws Exception {
		assertBadRequest(create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-14T18:00:00Z", 20, 4)),
				"INVALID_DATE_RANGE");
	}

	@Test
	void createRejectsBlankName() throws Exception {
		assertValidationError(create(body("   ", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 4)), "name");
	}

	@Test
	void createRejectsNegativeMinimumRest() throws Exception {
		assertValidationError(create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", -1, 4)),
				"minimumRestMinutes");
	}

	@Test
	void createRejectsZeroCourts() throws Exception {
		assertValidationError(create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 0)),
				"courtCount");
	}

	@Test
	void createRejectsMoreThanFiftyCourts() throws Exception {
		assertValidationError(create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 51)),
				"courtCount");
	}

	@Test
	void createRejectsMissingFields() throws Exception {
		create("{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors", hasSize(6)));
	}

	@Test
	void createRejectsMalformedDate() throws Exception {
		create(body("Open de Papeete", "not-a-date", "2026-11-16T04:00:00Z", 20, 4))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void findAllReturnsTournamentsMostRecentFirst() throws Exception {
		create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));
		create(body("Championnat de Polynésie", "2026-12-05T18:00:00Z", "2026-12-07T04:00:00Z", 20, 2));

		mvc.perform(get(URL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].name").value("Championnat de Polynésie"))
			.andExpect(jsonPath("$[1].name").value("Open de Papeete"));
	}

	@Test
	void findAllReturnsEmptyListWhenNoTournament() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void findByIdReturnsTournament() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		mvc.perform(get(URL + "/" + tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(tournamentId.toString()))
			.andExpect(jsonPath("$.name").value("Open de Papeete"));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownId() throws Exception {
		mvc.perform(get(URL + "/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void updateChangesOnlyProvidedFields() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		update(tournamentId, "{\"name\": \"  Open international de Papeete  \", \"minimumRestMinutes\": 30}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Open international de Papeete"))
			.andExpect(jsonPath("$.minimumRestMinutes").value(30))
			.andExpect(jsonPath("$.startsAt").value("2026-11-14T18:00:00Z"))
			.andExpect(jsonPath("$.endsAt").value("2026-11-16T04:00:00Z"))
			.andExpect(jsonPath("$.organizationId").value(organizationId.toString()));

		mvc.perform(get(URL + "/" + tournamentId))
			.andExpect(jsonPath("$.name").value("Open international de Papeete"))
			.andExpect(jsonPath("$.minimumRestMinutes").value(30));
	}

	@Test
	void updateRefreshesUpdatedAt() throws Exception {
		String created = create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2))
			.andReturn()
			.getResponse()
			.getContentAsString();
		UUID tournamentId = UUID.fromString(JsonPath.read(created, "$.id"));
		Instant createdAt = Instant.parse(JsonPath.read(created, "$.createdAt"));

		String updated = update(tournamentId, "{\"minimumRestMinutes\": 30}").andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(Instant.parse(JsonPath.<String>read(updated, "$.createdAt"))).isEqualTo(createdAt);
		assertThat(Instant.parse(JsonPath.<String>read(updated, "$.updatedAt"))).isAfter(createdAt);
	}

	@Test
	void updateChecksDateRangeAgainstExistingValues() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		assertBadRequest(update(tournamentId, "{\"endsAt\": \"2026-11-13T18:00:00Z\"}"), "INVALID_DATE_RANGE");
		mvc.perform(get(URL + "/" + tournamentId)).andExpect(jsonPath("$.endsAt").value("2026-11-16T04:00:00Z"));
	}

	@Test
	void updateRejectsBlankName() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		assertValidationError(update(tournamentId, "{\"name\": \"   \"}"), "name");
	}

	@Test
	void updateRejectsNegativeMinimumRest() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		assertValidationError(update(tournamentId, "{\"minimumRestMinutes\": -5}"), "minimumRestMinutes");
	}

	@Test
	void updateReturnsNotFoundForUnknownId() throws Exception {
		update(UUID.randomUUID(), "{\"name\": \"Open\"}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void deleteRemovesTournamentAndItsCourts() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", inDays(30), inDays(32), 20, 3));

		mvc.perform(delete(URL + "/" + tournamentId)).andExpect(status().isNoContent());

		mvc.perform(get(URL + "/" + tournamentId)).andExpect(status().isNotFound());
		Integer courts = jdbc.queryForObject("SELECT count(*) FROM court WHERE tournament_id = ?", Integer.class,
				tournamentId);
		assertThat(courts).isZero();
	}

	@Test
	void deleteRejectsStartedTournament() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", inDays(-1), inDays(1), 20, 2));

		mvc.perform(delete(URL + "/" + tournamentId))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("TOURNAMENT_ALREADY_STARTED"));
		mvc.perform(get(URL + "/" + tournamentId)).andExpect(status().isOk());
	}

	@Test
	void deleteReturnsNotFoundForUnknownId() throws Exception {
		mvc.perform(delete(URL + "/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	private static String inDays(int days) {
		return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
	}

	private ResultActions update(UUID tournamentId, String json) throws Exception {
		return mvc.perform(patch(URL + "/" + tournamentId).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String body(String name, String startsAt, String endsAt, int minimumRestMinutes, int courtCount) {
		return """
				{
				  "organizationId": "%s",
				  "name": "%s",
				  "startsAt": "%s",
				  "endsAt": "%s",
				  "minimumRestMinutes": %d,
				  "courtCount": %d
				}
				""".formatted(organizationId, name, startsAt, endsAt, minimumRestMinutes, courtCount);
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private UUID createAndGetId(String json) throws Exception {
		String response = create(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	private void assertNoTournamentCreated() {
		Integer count = jdbc.queryForObject("SELECT count(*) FROM tournament", Integer.class);
		assertThat(count).isZero();
	}

	private static void assertBadRequest(ResultActions result, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(code));
	}

	private static void assertValidationError(ResultActions result, String field) throws Exception {
		assertBadRequest(result, "VALIDATION_FAILED");
		result.andExpect(jsonPath("$.errors[*].field", hasItem(field)));
	}

}
