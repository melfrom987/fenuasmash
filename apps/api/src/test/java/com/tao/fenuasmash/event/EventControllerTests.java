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
class EventControllerTests {

	private static final List<String> ALL_DISCIPLINES = List.of("MS", "WS", "MD", "WD", "XD");

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
			.andExpect(jsonPath("$.discipline").value("MS"))
			.andExpect(jsonPath("$.categoryType").value("OPEN"))
			.andExpect(jsonPath("$.categoryLabel").doesNotExist());
	}

	@Test
	void eventNoLongerExposesFormatOrScoring() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated())
			.andExpect(jsonPath("$.format").doesNotExist())
			.andExpect(jsonPath("$.type").doesNotExist())
			.andExpect(jsonPath("$.scoring").doesNotExist());
	}

	@Test
	void createTrimsNameAndCategoryLabel() throws Exception {
		postJson(tournamentId, """
				{"name": "  Double Mixte  ", "discipline": "XD", "categoryType": "AGE", "categoryLabel": "  Cadet  "}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Double Mixte"))
			.andExpect(jsonPath("$.categoryType").value("AGE"))
			.andExpect(jsonPath("$.categoryLabel").value("Cadet"));
	}

	@Test
	void createAcceptsLevelCategory() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple D8/D9", "discipline": "MS", "categoryType": "LEVEL", "categoryLabel": "D8/D9"}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.categoryType").value("LEVEL"))
			.andExpect(jsonPath("$.categoryLabel").value("D8/D9"));
	}

	@Test
	void createClearsCategoryLabelWhenOpen() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple Homme", "discipline": "MS", "categoryType": "OPEN", "categoryLabel": "Cadet"}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.categoryType").value("OPEN"))
			.andExpect(jsonPath("$.categoryLabel").doesNotExist());
	}

	@Test
	void createRejectsMissingCategoryLabelWhenNotOpen() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Cadet", "discipline": "MS", "categoryType": "AGE"}
				"""), "categoryLabel");
	}

	@Test
	void createRejectsBlankCategoryLabelWhenNotOpen() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple D8", "discipline": "MS", "categoryType": "LEVEL", "categoryLabel": "   "}
				"""), "categoryLabel");
	}

	@Test
	void createRejectsCategoryLabelTooLong() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Cadet", "discipline": "MS", "categoryType": "AGE", "categoryLabel": "%s"}
				""".formatted("x".repeat(51))), "categoryLabel");
	}

	@Test
	void createRejectsUnknownCategoryType() throws Exception {
		postJson(tournamentId, """
				{"name": "Simple Cadet", "discipline": "MS", "categoryType": "SENIOR"}
				""").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
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
	void createRejectsMissingDiscipline() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Homme", "categoryType": "OPEN"}
				"""), "discipline");
	}

	@Test
	void createRejectsMissingCategoryType() throws Exception {
		assertValidationError(postJson(tournamentId, """
				{"name": "Simple Homme", "discipline": "MS"}
				"""), "categoryType");
	}

	@Test
	void createRejectsUnknownDiscipline() throws Exception {
		create(tournamentId, "Simple Homme", "XX").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createAcceptsOpenDouble() throws Exception {
		UUID openDouble = insertTournament("Open intergenre", "SINGLE_ELIMINATION", List.of("OD"));

		create(openDouble, "Double intergenre", "OD").andExpect(status().isCreated())
			.andExpect(jsonPath("$.discipline").value("OD"));
	}

	@Test
	void createRejectsDuplicateNameIgnoringCase() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated());

		create(tournamentId, "simple homme", "MS").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_NAME_ALREADY_EXISTS"));
	}

	@Test
	void createAllowsSameDisciplineInAnotherCategory() throws Exception {
		create(tournamentId, "Simple Homme Senior", "MS").andExpect(status().isCreated());

		postJson(tournamentId, """
				{"name": "Simple Homme Cadet", "discipline": "MS", "categoryType": "AGE", "categoryLabel": "Cadet"}
				""").andExpect(status().isCreated());
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
			.andExpect(jsonPath("$[0].discipline").value("MS"))
			.andExpect(jsonPath("$[1].discipline").value("WD"))
			.andExpect(jsonPath("$[2].discipline").value("XD"));
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
			.andExpect(jsonPath("$.discipline").value("MD"))
			.andExpect(jsonPath("$.categoryType").value("OPEN"));

		update(eventId, "{\"discipline\": \"WD\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Double Homme Vétérans"))
			.andExpect(jsonPath("$.discipline").value("WD"));

		update(eventId, "{\"categoryType\": \"AGE\", \"categoryLabel\": \"Vétéran\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.discipline").value("WD"))
			.andExpect(jsonPath("$.categoryType").value("AGE"))
			.andExpect(jsonPath("$.categoryLabel").value("Vétéran"));

		update(eventId, "{\"name\": \"Double Dame Vétérans\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.categoryType").value("AGE"))
			.andExpect(jsonPath("$.categoryLabel").value("Vétéran"));
	}

	@Test
	void updateChangesOnlyCategoryLabel() throws Exception {
		UUID eventId = createAgeEvent();

		update(eventId, "{\"categoryLabel\": \"  Minime  \"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.categoryType").value("AGE"))
			.andExpect(jsonPath("$.categoryLabel").value("Minime"));
	}

	@Test
	void updateToOpenClearsCategoryLabel() throws Exception {
		UUID eventId = createAgeEvent();

		update(eventId, "{\"categoryType\": \"OPEN\", \"categoryLabel\": null}").andExpect(status().isOk())
			.andExpect(jsonPath("$.categoryType").value("OPEN"))
			.andExpect(jsonPath("$.categoryLabel").doesNotExist());
	}

	@Test
	void updateToAgeRequiresCategoryLabel() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		assertValidationError(update(eventId, "{\"categoryType\": \"AGE\"}"), "categoryLabel");
	}

	@Test
	void updateRejectsNullCategoryLabelWhenTypeIsSent() throws Exception {
		UUID eventId = createAgeEvent();

		assertValidationError(update(eventId, "{\"categoryType\": \"LEVEL\", \"categoryLabel\": null}"),
				"categoryLabel");
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
	void updateRejectsBlankCategoryLabelWhenNotOpen() throws Exception {
		UUID eventId = createAgeEvent();

		assertValidationError(update(eventId, "{\"categoryLabel\": \"  \"}"), "categoryLabel");
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
	void createHasNoPoolStructureForSingleElimination() throws Exception {
		create(tournamentId, "Simple Homme", "MS").andExpect(status().isCreated())
			.andExpect(jsonPath("$.poolCount").doesNotExist())
			.andExpect(jsonPath("$.poolSize").doesNotExist())
			.andExpect(jsonPath("$.qualifiersPerPool").doesNotExist());
	}

	@Test
	void createWithPoolStructure() throws Exception {
		postPools(insertPoolsTournament(), 4, 4, 2).andExpect(status().isCreated())
			.andExpect(jsonPath("$.poolCount").value(4))
			.andExpect(jsonPath("$.poolSize").value(4))
			.andExpect(jsonPath("$.qualifiersPerPool").value(2));
	}

	@Test
	void createRejectsPoolsTournamentWithoutStructure() throws Exception {
		assertBadRequest(create(insertPoolsTournament(), "Simple Dame", "WS"), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsQualifiersNotLessThanPoolSize() throws Exception {
		assertBadRequest(postPools(insertPoolsTournament(), 2, 4, 4), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsPoolStructureForSingleElimination() throws Exception {
		assertBadRequest(postPools(tournamentId, 2, 4, 2), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsPoolStructureForSinglePool() throws Exception {
		UUID singlePool = insertTournament("Poule unique", "SINGLE_POOL", ALL_DISCIPLINES);

		assertBadRequest(postPools(singlePool, 1, 4, 2), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void createRejectsPoolSizeOutOfRange() throws Exception {
		assertValidationError(postPools(insertPoolsTournament(), 2, 9, 2), "poolSize");
	}

	@Test
	void createRejectsPoolCountOutOfRange() throws Exception {
		assertValidationError(postPools(insertPoolsTournament(), 0, 4, 2), "poolCount");
	}

	@Test
	void updateKeepsPoolStructureWhenChangingOnlyOneValue() throws Exception {
		String response = postPools(insertPoolsTournament(), 4, 4, 2).andReturn().getResponse().getContentAsString();
		UUID eventId = UUID.fromString(JsonPath.read(response, "$.id"));

		update(eventId, "{\"poolCount\": 3}").andExpect(status().isOk())
			.andExpect(jsonPath("$.poolCount").value(3))
			.andExpect(jsonPath("$.poolSize").value(4))
			.andExpect(jsonPath("$.qualifiersPerPool").value(2));
	}

	@Test
	void updateRejectsQualifiersNotLessThanExistingPoolSize() throws Exception {
		String response = postPools(insertPoolsTournament(), 4, 4, 2).andReturn().getResponse().getContentAsString();
		UUID eventId = UUID.fromString(JsonPath.read(response, "$.id"));

		assertBadRequest(update(eventId, "{\"qualifiersPerPool\": 4}"), "INVALID_FORMAT_SETTINGS");
	}

	@Test
	void updateRejectsPoolStructureForSingleElimination() throws Exception {
		UUID eventId = createAndGetId(tournamentId, "Simple Homme", "MS");

		assertBadRequest(update(eventId, "{\"poolSize\": 4}"), "INVALID_FORMAT_SETTINGS");
	}

	private ResultActions postPools(UUID tournament, int poolCount, int poolSize, int qualifiers) throws Exception {
		return postJson(tournament, """
				{"name": "Simple Dame", "discipline": "WS", "categoryType": "OPEN",
				 "poolCount": %d, "poolSize": %d, "qualifiersPerPool": %d}
				""".formatted(poolCount, poolSize, qualifiers));
	}

	private static void assertBadRequest(ResultActions result, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value(code));
	}

	private UUID insertTournament(String name) {
		return insertTournament(name, "SINGLE_ELIMINATION", ALL_DISCIPLINES);
	}

	private UUID insertPoolsTournament() {
		return insertTournament("Open en poules", "POOLS_THEN_ELIMINATION", ALL_DISCIPLINES);
	}

	private UUID insertTournament(String name, String drawFormat, List<String> disciplines) {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("""
				INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
				    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
				    created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, 'INDIVIDUAL', ?, 'SETS', 2, 21, TRUE, 30, ?, ?)
				""", id, organizationId, name, Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), 20, drawFormat, Timestamp.from(now),
				Timestamp.from(now));
		for (int position = 0; position < disciplines.size(); position++) {
			jdbc.update("INSERT INTO tournament_discipline (tournament_id, sort_order, discipline) VALUES (?, ?, ?)", id,
					position, disciplines.get(position));
		}
		return id;
	}

	private static String eventsUrl(UUID tournamentId) {
		return "/api/tournaments/" + tournamentId + "/events";
	}

	private ResultActions create(UUID tournamentId, String name, String discipline) throws Exception {
		return postJson(tournamentId,
				"{\"name\": \"%s\", \"discipline\": \"%s\", \"categoryType\": \"OPEN\"}".formatted(name, discipline));
	}

	private UUID createAgeEvent() throws Exception {
		String response = postJson(tournamentId, """
				{"name": "Simple Cadet", "discipline": "MS", "categoryType": "AGE", "categoryLabel": "Cadet"}
				""").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	private ResultActions postJson(UUID tournamentId, String json) throws Exception {
		return mvc.perform(post(eventsUrl(tournamentId)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private UUID createAndGetId(UUID tournamentId, String name, String discipline) throws Exception {
		String response = create(tournamentId, name, discipline).andExpect(status().isCreated())
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
