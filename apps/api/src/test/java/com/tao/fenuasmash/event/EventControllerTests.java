package com.tao.fenuasmash.event;

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
class EventControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	private UUID tournamentId;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
		tournamentId = insertTournament("Open de Papeete");
	}

	@Test
	void createReturnsCreatedEventWithLocation() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/events/")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$.name").value("Simple Homme"))
			.andExpect(jsonPath("$.type").value("MS"));
	}

	@Test
	void createTrimsName() throws Exception {
		create(tournamentId, "  Double Mixte  ", "XD").andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Double Mixte"));
	}

	@Test
	void createReturnsNotFoundForUnknownTournament() throws Exception {
		create(UUID.randomUUID(), "Simple Homme", "MS").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void createRejectsBlankName() throws Exception {
		assertValidationError(create(tournamentId, "   ", "MS"), "name");
	}

	@Test
	void createRejectsMissingType() throws Exception {
		assertValidationError(postJson(tournamentId, "{\"name\": \"Simple Homme\"}"), "type");
	}

	@Test
	void createRejectsUnknownType() throws Exception {
		create(tournamentId, "Simple Homme", "XX").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createRejectsDuplicateNameIgnoringCase() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated());

		create(tournamentId, "simple homme", "MS").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_NAME_ALREADY_EXISTS"));
	}

	@Test
	void createAllowsSameNameInAnotherTournament() throws Exception {
		UUID otherTournamentId = insertTournament("Championnat de Polynésie");
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated());

		create(otherTournamentId, "Simple Homme", "MS").andExpect(status().isCreated());
	}

	@Test
	void findAllReturnsEventsOfTournamentInOfficialOrder() throws Exception {
		create(tournamentId, "Double Mixte", "XD");
		create(tournamentId, "Simple Homme", "MS");
		create(tournamentId, "Double Dame", "WD");
		create(insertTournament("Championnat de Polynésie"), "Simple Dame", "WS");

		mvc.perform(get(eventsUrl(tournamentId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].type").value("MS"))
			.andExpect(jsonPath("$[1].type").value("WD"))
			.andExpect(jsonPath("$[2].type").value("XD"));
	}

	@Test
	void findAllReturnsNotFoundForUnknownTournament() throws Exception {
		mvc.perform(get(eventsUrl(UUID.randomUUID())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void findByIdReturnsEvent() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		mvc.perform(get("/api/events/" + eventId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(eventId.toString()))
			.andExpect(jsonPath("$.name").value("Simple Homme"));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownId() throws Exception {
		mvc.perform(get("/api/events/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void updateChangesOnlyProvidedFields() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Double Homme", "MD");

		update(eventId, "{\"name\": \"Double Homme Vétérans\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Double Homme Vétérans"))
			.andExpect(jsonPath("$.type").value("MD"));

		update(eventId, "{\"type\": \"WD\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Double Homme Vétérans"))
			.andExpect(jsonPath("$.type").value("WD"));
	}

	@Test
	void updateAllowsChangingCaseOfOwnName() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "simple homme", "MS");

		update(eventId, "{\"name\": \"Simple Homme\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Simple Homme"));
	}

	@Test
	void updateRejectsNameUsedByAnotherEvent() throws Exception {
		createAndGetId(tournamentId, "Simple Homme", "MS");
		UUID eventId = createAndGetId(tournamentId, "Simple Dame", "WS");

		update(eventId, "{\"name\": \"Simple Homme\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_NAME_ALREADY_EXISTS"));
	}

	@Test
	void updateRejectsBlankName() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		assertValidationError(update(eventId, "{\"name\": \"  \"}"), "name");
	}

	@Test
	void updateReturnsNotFoundForUnknownId() throws Exception {
		update(UUID.randomUUID(), "{\"name\": \"Simple Homme\"}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void deleteRemovesEvent() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		mvc.perform(delete("/api/events/" + eventId)).andExpect(status().isNoContent());

		mvc.perform(get("/api/events/" + eventId)).andExpect(status().isNotFound());
	}

	@Test
	void deleteReturnsNotFoundForUnknownId() throws Exception {
		mvc.perform(delete("/api/events/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void deletingTournamentDeletesItsEvents() throws Exception {
		createAndGetId(tournamentId, "Simple Homme", "MS");

		mvc.perform(delete("/api/tournaments/" + tournamentId)).andExpect(status().isNoContent());

		Integer events = jdbc.queryForObject("SELECT count(*) FROM event WHERE tournament_id = ?", Integer.class,
				tournamentId);
		assertThat(events).isZero();
	}

	@Test
	void createUsesSingleEliminationAndStandardScoringByDefault() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated())
			.andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
			.andExpect(jsonPath("$.poolSize").doesNotExist())
			.andExpect(jsonPath("$.qualifiersPerPool").doesNotExist())
			.andExpect(jsonPath("$.scoring.gamesToWin").value(2))
			.andExpect(jsonPath("$.scoring.pointsToWin").value(21))
			.andExpect(jsonPath("$.scoring.pointCap").value(30));
	}

	@Test
	void createWithPoolsFormat() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple Dame", "type": "WS", "format": "POOLS_THEN_ELIMINATION",
				 "poolSize": 4, "qualifiersPerPool": 2}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.format").value("POOLS_THEN_ELIMINATION"))
			.andExpect(jsonPath("$.poolSize").value(4))
			.andExpect(jsonPath("$.qualifiersPerPool").value(2));
	}

	@Test
	void createWithRoundRobinAndCustomScoring() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple Jeunes", "type": "MS", "format": "ROUND_ROBIN",
				 "scoring": {"gamesToWin": 1, "pointsToWin": 15, "pointCap": 15}}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.format").value("ROUND_ROBIN"))
			.andExpect(jsonPath("$.scoring.gamesToWin").value(1))
			.andExpect(jsonPath("$.scoring.pointsToWin").value(15))
			.andExpect(jsonPath("$.scoring.pointCap").value(15));
	}

	@Test
	void createRejectsPoolsFormatWithoutPoolSettings() throws Exception {
		assertBadRequest(postJson(tournamentId, """
				{"name": "Simple Dame", "type": "WS", "format": "POOLS_THEN_ELIMINATION"}
				"""), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsQualifiersNotLessThanPoolSize() throws Exception {
		assertBadRequest(postJson(tournamentId, """
				{"name": "Simple Dame", "type": "WS", "format": "POOLS_THEN_ELIMINATION",
				 "poolSize": 4, "qualifiersPerPool": 4}
				"""), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsPoolSettingsForOtherFormats() throws Exception {
		assertBadRequest(postJson(tournamentId, """
				{"name": "Simple Dame", "type": "WS", "format": "ROUND_ROBIN", "poolSize": 4}
				"""), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsPoolSizeOutOfRange() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Dame", "type": "WS", "format": "POOLS_THEN_ELIMINATION",
				 "poolSize": 9, "qualifiersPerPool": 2}
				"""), "poolSize");
	}

	@Test
	void createRejectsPointCapBelowPointsToWin() throws Exception {
		assertBadRequest(postJson(tournamentId, """
				{"name": "Simple Homme", "type": "MS", "scoring": {"pointsToWin": 21, "pointCap": 20}}
				"""), "INVALID_SCORING_RULES");
	}

	@Test
	void createRejectsGamesToWinOutOfRange() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Homme", "type": "MS", "scoring": {"gamesToWin": 4}}
				"""), "scoring.gamesToWin");
	}

	@Test
	void createRejectsUnknownFormat() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple Homme", "type": "MS", "format": "SWISS"}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void updateToPoolsRequiresPoolSettings() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		assertBadRequest(update(eventId, "{\"format\": \"POOLS_THEN_ELIMINATION\"}"), "INVALID_FORMAT_SETTINGS");

		update(eventId, """
				{"format": "POOLS_THEN_ELIMINATION", "poolSize": 3, "qualifiersPerPool": 1}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.format").value("POOLS_THEN_ELIMINATION"))
			.andExpect(jsonPath("$.poolSize").value(3))
			.andExpect(jsonPath("$.qualifiersPerPool").value(1));
	}

	@Test
	void updateKeepsPoolSettingsWhenChangingOnlyOneOfThem() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");
		update(eventId, """
				{"format": "POOLS_THEN_ELIMINATION", "poolSize": 4, "qualifiersPerPool": 2}
				""");

		update(eventId, "{\"poolSize\": 5}").andExpect(status().isOk())
			.andExpect(jsonPath("$.poolSize").value(5))
			.andExpect(jsonPath("$.qualifiersPerPool").value(2));
	}

	@Test
	void updateToAnotherFormatClearsPoolSettings() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");
		update(eventId, """
				{"format": "POOLS_THEN_ELIMINATION", "poolSize": 4, "qualifiersPerPool": 2}
				""");

		update(eventId, "{\"format\": \"SINGLE_ELIMINATION\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"))
			.andExpect(jsonPath("$.poolSize").doesNotExist())
			.andExpect(jsonPath("$.qualifiersPerPool").doesNotExist());
	}

	@Test
	void updateChangesOnlyProvidedScoringFields() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		update(eventId, "{\"scoring\": {\"pointsToWin\": 15, \"pointCap\": 21}}").andExpect(status().isOk())
			.andExpect(jsonPath("$.scoring.gamesToWin").value(2))
			.andExpect(jsonPath("$.scoring.pointsToWin").value(15))
			.andExpect(jsonPath("$.scoring.pointCap").value(21));
	}

	@Test
	void updateRejectsPointCapBelowExistingPointsToWin() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		assertBadRequest(update(eventId, "{\"scoring\": {\"pointCap\": 15}}"), "INVALID_SCORING_RULES");
		mvc.perform(get("/api/events/" + eventId)).andExpect(jsonPath("$.scoring.pointCap").value(30));
	}

	private static void assertBadRequest(ResultActions result, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(code));
	}

	private UUID insertTournament(String name) {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update(
				"INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes, created_at, updated_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				id, organizationId, name, Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), 20, Timestamp.from(now), Timestamp.from(now));
		return id;
	}

	private static String eventsUrl(UUID tournamentId) {
		return "/api/tournaments/" + tournamentId + "/events";
	}

	private ResultActions create(UUID tournamentId, String name, String type) throws Exception {
		return postJson(tournamentId, "{\"name\": \"%s\", \"type\": \"%s\"}".formatted(name, type));
	}

	private ResultActions postJson(UUID tournamentId, String json) throws Exception {
		return mvc.perform(post(eventsUrl(tournamentId)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private UUID createAndGetId(UUID tournamentId, String name, String type) throws Exception {
		String response = create(tournamentId, name, type).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	private ResultActions update(UUID eventId, String json) throws Exception {
		return mvc.perform(patch("/api/events/" + eventId).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static void assertValidationError(ResultActions result, String field) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem(field)));
	}

}
