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

	private static final String STANDARD_SCORING = """
			{"gamesToWin": 2, "pointsToWin": 21, "twoPointLead": true, "pointCap": 30}""";

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
			.andExpect(jsonPath("$.errors", hasSize(11)));
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

	@Test
	void createStoresAndReturnsRules() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		mvc.perform(get(URL + "/" + tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.competitionType").value("INDIVIDUAL"))
			.andExpect(jsonPath("$.drawFormat").value("POOLS_THEN_ELIMINATION"))
			.andExpect(jsonPath("$.playMode").value("SETS"))
			.andExpect(jsonPath("$.scoring.gamesToWin").value(2))
			.andExpect(jsonPath("$.scoring.pointsToWin").value(21))
			.andExpect(jsonPath("$.scoring.twoPointLead").value(true))
			.andExpect(jsonPath("$.scoring.pointCap").value(30))
			.andExpect(jsonPath("$.relayStep").doesNotExist())
			.andExpect(jsonPath("$.disciplines[0]").value("XD"))
			.andExpect(jsonPath("$.disciplines[1]").value("MS"))
			.andExpect(jsonPath("$.disciplines[2]").value("WS"));
	}

	@Test
	void findAllReturnsRules() throws Exception {
		create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		mvc.perform(get(URL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].disciplines", hasSize(3)))
			.andExpect(jsonPath("$[0].drawFormat").value("POOLS_THEN_ELIMINATION"));
	}

	@Test
	void createAcceptsFirstToScoreWithoutCap() throws Exception {
		create(bodyWithRules("SETS", """
				{"gamesToWin": 1, "pointsToWin": 15, "twoPointLead": false}""", null))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.scoring.twoPointLead").value(false))
			.andExpect(jsonPath("$.scoring.pointCap").doesNotExist());
	}

	@Test
	void createAcceptsRelayWithStep() throws Exception {
		create(bodyWithRules("RELAY", """
				{"gamesToWin": 1, "pointsToWin": 49, "twoPointLead": true, "pointCap": 50}""", 7))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.playMode").value("RELAY"))
			.andExpect(jsonPath("$.relayStep").value(7));
	}

	@Test
	void createRejectsRelayWithoutStep() throws Exception {
		assertBadRequest(create(bodyWithRules("RELAY", STANDARD_SCORING, null)), "INVALID_RELAY_SETTINGS");
	}

	@Test
	void createRejectsRelayStepInSetsMode() throws Exception {
		assertBadRequest(create(bodyWithRules("SETS", STANDARD_SCORING, 7)), "INVALID_RELAY_SETTINGS");
	}

	@Test
	void createRejectsTwoPointLeadWithoutCap() throws Exception {
		assertBadRequest(create(bodyWithRules("SETS", """
				{"gamesToWin": 2, "pointsToWin": 21, "twoPointLead": true}""", null)), "INVALID_SCORING_RULES");
	}

	@Test
	void createRejectsCapWithoutTwoPointLead() throws Exception {
		assertBadRequest(create(bodyWithRules("SETS", """
				{"gamesToWin": 2, "pointsToWin": 21, "twoPointLead": false, "pointCap": 30}""", null)),
				"INVALID_SCORING_RULES");
	}

	@Test
	void createRejectsCapBelowPointsToWin() throws Exception {
		assertBadRequest(create(bodyWithRules("SETS", """
				{"gamesToWin": 2, "pointsToWin": 21, "twoPointLead": true, "pointCap": 20}""", null)),
				"INVALID_SCORING_RULES");
	}

	@Test
	void createRejectsGamesToWinOutOfRange() throws Exception {
		assertValidationError(create(bodyWithRules("SETS", """
				{"gamesToWin": 4, "pointsToWin": 21, "twoPointLead": true, "pointCap": 30}""", null)),
				"scoring.gamesToWin");
	}

	@Test
	void createRejectsMissingScoringFields() throws Exception {
		assertValidationError(create(bodyWithRules("SETS", "{}", null)), "scoring.pointsToWin");
	}

	@Test
	void createRejectsUnknownDrawFormat() throws Exception {
		create(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2)
			.replace("POOLS_THEN_ELIMINATION", "SWISS")).andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsEmptyDisciplines() throws Exception {
		assertValidationError(create(bodyWithDisciplines("[]")), "disciplines");
	}

	@Test
	void updateChangesOnlyProvidedScoringFields() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		update(tournamentId, "{\"scoring\": {\"pointsToWin\": 15, \"pointCap\": 21}}").andExpect(status().isOk())
			.andExpect(jsonPath("$.scoring.gamesToWin").value(2))
			.andExpect(jsonPath("$.scoring.pointsToWin").value(15))
			.andExpect(jsonPath("$.scoring.twoPointLead").value(true))
			.andExpect(jsonPath("$.scoring.pointCap").value(21));
	}

	@Test
	void updateToFirstToScoreClearsCap() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		update(tournamentId, "{\"scoring\": {\"twoPointLead\": false}}").andExpect(status().isOk())
			.andExpect(jsonPath("$.scoring.twoPointLead").value(false))
			.andExpect(jsonPath("$.scoring.pointCap").doesNotExist());
	}

	@Test
	void updateRejectsCapBelowExistingPointsToWin() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		assertBadRequest(update(tournamentId, "{\"scoring\": {\"pointCap\": 15}}"), "INVALID_SCORING_RULES");
		mvc.perform(get(URL + "/" + tournamentId)).andExpect(jsonPath("$.scoring.pointCap").value(30));
	}

	@Test
	void updateToRelayRequiresStep() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		assertBadRequest(update(tournamentId, "{\"playMode\": \"RELAY\"}"), "INVALID_RELAY_SETTINGS");

		update(tournamentId, "{\"playMode\": \"RELAY\", \"relayStep\": 7}").andExpect(status().isOk())
			.andExpect(jsonPath("$.relayStep").value(7));
	}

	@Test
	void updateBackToSetsClearsRelayStep() throws Exception {
		UUID tournamentId = createAndGetId(bodyWithRules("RELAY", STANDARD_SCORING, 7));

		update(tournamentId, "{\"playMode\": \"SETS\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.relayStep").doesNotExist());
	}

	@Test
	void updateReplacesDisciplinesInGivenOrder() throws Exception {
		UUID tournamentId = createAndGetId(body("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2));

		update(tournamentId, "{\"disciplines\": [\"WS\", \"OD\"]}").andExpect(status().isOk());

		mvc.perform(get(URL + "/" + tournamentId))
			.andExpect(jsonPath("$.disciplines", hasSize(2)))
			.andExpect(jsonPath("$.disciplines[0]").value("WS"))
			.andExpect(jsonPath("$.disciplines[1]").value("OD"));
	}

	private static String inDays(int days) {
		return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
	}

	private ResultActions update(UUID tournamentId, String json) throws Exception {
		return mvc.perform(patch(URL + "/" + tournamentId).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String body(String name, String startsAt, String endsAt, int minimumRestMinutes, int courtCount) {
		return bodyOf(name, startsAt, endsAt, minimumRestMinutes, courtCount, "SETS", STANDARD_SCORING, null,
				"[\"XD\", \"MS\", \"WS\"]");
	}

	private String bodyWithRules(String playMode, String scoring, Integer relayStep) {
		return bodyOf("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2, playMode, scoring,
				relayStep, "[\"XD\", \"MS\", \"WS\"]");
	}

	private String bodyWithDisciplines(String disciplines) {
		return bodyOf("Open de Papeete", "2026-11-14T18:00:00Z", "2026-11-16T04:00:00Z", 20, 2, "SETS",
				STANDARD_SCORING, null, disciplines);
	}

	private String bodyOf(String name, String startsAt, String endsAt, int minimumRestMinutes, int courtCount,
			String playMode, String scoring, Integer relayStep, String disciplines) {
		return """
				{
				  "organizationId": "%s",
				  "name": "%s",
				  "startsAt": "%s",
				  "endsAt": "%s",
				  "minimumRestMinutes": %d,
				  "courtCount": %d,
				  "competitionType": "INDIVIDUAL",
				  "drawFormat": "POOLS_THEN_ELIMINATION",
				  "playMode": "%s",
				  "scoring": %s,
				  "relayStep": %s,
				  "disciplines": %s
				}
				""".formatted(organizationId, name, startsAt, endsAt, minimumRestMinutes, courtCount, playMode, scoring,
				relayStep, disciplines);
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
