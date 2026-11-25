package com.tao.fenuasmash.match;

import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.common.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MatchControllerTests {

	private static final UUID ALICE = UUID.randomUUID();

	private static final UUID BOB = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	private UUID tournamentId;

	private UUID eventId;

	@BeforeEach
	void setUp() throws Exception {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
		tournamentId = insertTournament("Open de Papeete");
		eventId = createEvent(tournamentId, "Simple Hommes");
	}

	// ------------------------------------------------------------ Creation and reads

	@Test
	void createReturnsScheduledMatchWithoutResult() throws Exception {
		create(eventId, 1, ALICE, BOB).andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/matches/")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$.eventId").value(eventId.toString()))
			.andExpect(jsonPath("$.round").value(1))
			.andExpect(jsonPath("$.sideA").value(ALICE.toString()))
			.andExpect(jsonPath("$.sideB").value(BOB.toString()))
			.andExpect(jsonPath("$.status").value("SCHEDULED"))
			.andExpect(jsonPath("$.winner").isEmpty())
			.andExpect(jsonPath("$.outcome").isEmpty())
			.andExpect(jsonPath("$.stateVersion").value(0))
			.andExpect(jsonPath("$.startedAt").isEmpty())
			.andExpect(jsonPath("$.endedAt").isEmpty())
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void createAcceptsUnknownSides() throws Exception {
		create(eventId, null, null, null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.round").isEmpty())
			.andExpect(jsonPath("$.sideA").isEmpty())
			.andExpect(jsonPath("$.sideB").isEmpty());
	}

	@Test
	void createRejectsTheSameRegistrationOnBothSides() throws Exception {
		create(eventId, 1, ALICE, ALICE).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_SIDES"));
	}

	@Test
	void createValidatesTheRound() throws Exception {
		create(eventId, 0, ALICE, BOB).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("round"));
	}

	@Test
	void createReturnsNotFoundForUnknownEvent() throws Exception {
		create(UUID.randomUUID(), 1, ALICE, BOB).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void onlyOrganizersCreateAndMoveMatches() throws Exception {
		UUID matchId = createMatch();
		String body = "{\"status\": \"READY\", \"stateVersion\": 0}";

		mvc.perform(post("/api/events/{id}/matches", eventId).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/events/{id}/matches", eventId).with(club(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isForbidden());
		mvc.perform(post("/api/matches/{id}/status", matchId).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/matches/{id}/status", matchId).with(club(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isForbidden());
	}

	@Test
	void readsArePublic() throws Exception {
		UUID matchId = createMatch();

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(matchId.toString()));
		mvc.perform(get("/api/events/{id}/matches", eventId)).andExpect(status().isOk());
		mvc.perform(get("/api/tournaments/{id}/matches", tournamentId)).andExpect(status().isOk());
	}

	@Test
	void eventMatchesAreSortedByRoundThenCreation() throws Exception {
		UUID noRound = createMatch(eventId, null);
		UUID secondRound = createMatch(eventId, 2);
		UUID firstRound = createMatch(eventId, 1);
		UUID firstRoundLater = createMatch(eventId, 1);
		createMatch(createEvent(tournamentId, "Simple Dames"), 1);

		mvc.perform(get("/api/events/{id}/matches", eventId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(4)))
			.andExpect(jsonPath("$[0].id").value(firstRound.toString()))
			.andExpect(jsonPath("$[1].id").value(firstRoundLater.toString()))
			.andExpect(jsonPath("$[2].id").value(secondRound.toString()))
			.andExpect(jsonPath("$[3].id").value(noRound.toString()));
	}

	@Test
	void tournamentMatchesCanBeFilteredByStatus() throws Exception {
		UUID ready = createMatch();
		changeStatus(ready, "READY", 0).andExpect(status().isOk());
		UUID called = createMatch(createEvent(tournamentId, "Simple Dames"), 1);
		changeStatus(called, "READY", 0).andExpect(status().isOk());
		callTo(called, addCourt(), 1).andExpect(status().isOk());
		createMatch();
		createMatch(createEvent(insertTournament("Open de Moorea"), "Simple Hommes"), 1);

		mvc.perform(get("/api/tournaments/{id}/matches", tournamentId)).andExpect(jsonPath("$", hasSize(3)));
		mvc.perform(get("/api/tournaments/{id}/matches?status=READY&status=CALLED", tournamentId))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].status").value("READY"))
			.andExpect(jsonPath("$[1].status").value("CALLED"));
	}

	@Test
	void listsReturnNotFoundForUnknownParents() throws Exception {
		mvc.perform(get("/api/events/{id}/matches", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
		mvc.perform(get("/api/tournaments/{id}/matches", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
		mvc.perform(get("/api/matches/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
	}

	// ------------------------------------------------------------ Transitions

	@Test
	void matchGoesThroughEveryStepUntilItIsWon() throws Exception {
		UUID matchId = createMatch();

		changeStatus(matchId, "READY", 0).andExpect(status().isOk()).andExpect(jsonPath("$.stateVersion").value(1));
		UUID court = addCourt();
		callTo(matchId, court, 1).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CALLED"))
			.andExpect(jsonPath("$.courtId").value(court.toString()));
		changeStatus(matchId, "WARMUP", 2).andExpect(status().isOk());
		changeStatus(matchId, "IN_PROGRESS", 3).andExpect(status().isOk())
			.andExpect(jsonPath("$.startedAt").isNotEmpty())
			.andExpect(jsonPath("$.endedAt").isEmpty());
		changeStatus(matchId, "FINISHED", 4, "A", "NORMAL").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.winner").value("A"))
			.andExpect(jsonPath("$.outcome").value("NORMAL"))
			.andExpect(jsonPath("$.endedAt").isNotEmpty())
			.andExpect(jsonPath("$.stateVersion").value(5));
	}

	@Test
	void invalidTransitionIsRejectedAndChangesNothing() throws Exception {
		UUID matchId = createMatch();

		changeStatus(matchId, "IN_PROGRESS", 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		changeStatus(matchId, "SCHEDULED", 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.status").value("SCHEDULED"))
			.andExpect(jsonPath("$.stateVersion").value(0));
	}

	@Test
	void matchCanStepBackBeforeItStarts() throws Exception {
		UUID matchId = moveToWarmup(createMatch());

		changeStatus(matchId, "CALLED", 3).andExpect(status().isOk()).andExpect(jsonPath("$.courtId").isNotEmpty());
		changeStatus(matchId, "READY", 4).andExpect(status().isOk()).andExpect(jsonPath("$.courtId").isEmpty());
		changeStatus(matchId, "SCHEDULED", 5).andExpect(status().isOk());
	}

	@Test
	void matchInProgressCanOnlyBeFinished() throws Exception {
		UUID matchId = moveToInProgress(createMatch());

		for (String status : List.of("SCHEDULED", "READY", "CALLED", "WARMUP", "CANCELLED", "WALKOVER")) {
			changeStatus(matchId, status, 4, status.equals("WALKOVER") ? "A" : null, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		}
	}

	@Test
	void cancelledAndWalkoverAreFinal() throws Exception {
		UUID cancelled = createMatch();
		changeStatus(cancelled, "CANCELLED", 0).andExpect(status().isOk())
			.andExpect(jsonPath("$.endedAt").isNotEmpty())
			.andExpect(jsonPath("$.winner").isEmpty());
		UUID walkover = createMatch();
		changeStatus(walkover, "WALKOVER", 0, "B", null).andExpect(status().isOk());

		for (String status : List.of("SCHEDULED", "READY", "IN_PROGRESS")) {
			changeStatus(cancelled, status, 1).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
			changeStatus(walkover, status, 1).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		}
	}

	@Test
	void matchNeedsBothSidesToGoFurther() throws Exception {
		UUID matchId = jsonId(create(eventId, 1, ALICE, null));

		changeStatus(matchId, "READY", 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_SIDES_INCOMPLETE"));
		changeStatus(matchId, "WALKOVER", 0, "A", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_SIDES_INCOMPLETE"));
		changeStatus(matchId, "CANCELLED", 0).andExpect(status().isOk());
	}

	@Test
	void finishedMatchCanBeReopenedAndLosesItsResult() throws Exception {
		UUID matchId = moveToInProgress(createMatch());
		String started = JsonPath.read(getMatch(matchId), "$.startedAt");
		changeStatus(matchId, "FINISHED", 4, "B", "NORMAL").andExpect(status().isOk());

		changeStatus(matchId, "IN_PROGRESS", 5).andExpect(status().isOk())
			.andExpect(jsonPath("$.winner").isEmpty())
			.andExpect(jsonPath("$.outcome").isEmpty())
			.andExpect(jsonPath("$.endedAt").isEmpty())
			.andExpect(jsonPath("$.startedAt").value(started));
	}

	// ------------------------------------------------------------ Results

	@Test
	void retirementProducesAResult() throws Exception {
		UUID matchId = moveToInProgress(createMatch());

		changeStatus(matchId, "FINISHED", 4, "A", "RETIRED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.winner").value("A"))
			.andExpect(jsonPath("$.outcome").value("RETIRED"));
	}

	@Test
	void walkoverProducesAResult() throws Exception {
		UUID matchId = moveToWarmup(createMatch());

		changeStatus(matchId, "WALKOVER", 3, "B", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("WALKOVER"))
			.andExpect(jsonPath("$.winner").value("B"))
			.andExpect(jsonPath("$.outcome").value("WALKOVER"))
			.andExpect(jsonPath("$.startedAt").isEmpty())
			.andExpect(jsonPath("$.endedAt").isNotEmpty());
	}

	@Test
	void finishedMatchNeedsAWinnerAndAnOutcome() throws Exception {
		UUID matchId = moveToInProgress(createMatch());

		changeStatus(matchId, "FINISHED", 4).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
		changeStatus(matchId, "FINISHED", 4, "A", null).andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
		changeStatus(matchId, "FINISHED", 4, null, "NORMAL")
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
		changeStatus(matchId, "FINISHED", 4, "A", "WALKOVER")
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
	}

	@Test
	void walkoverNeedsAWinner() throws Exception {
		UUID matchId = createMatch();

		changeStatus(matchId, "WALKOVER", 0).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
		changeStatus(matchId, "WALKOVER", 0, "A", "RETIRED")
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
	}

	@Test
	void otherStatusesTakeNoResult() throws Exception {
		UUID matchId = createMatch();

		changeStatus(matchId, "READY", 0, "A", null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
		changeStatus(matchId, "CANCELLED", 0, null, "NORMAL")
			.andExpect(jsonPath("$.code").value("INVALID_MATCH_RESULT"));
	}

	@Test
	void changeStatusValidatesTheRequest() throws Exception {
		UUID matchId = createMatch();

		mvc.perform(post("/api/matches/{id}/status", matchId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"READY\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("stateVersion"));
	}

	@Test
	void changeStatusReturnsNotFoundForUnknownMatch() throws Exception {
		changeStatus(UUID.randomUUID(), "READY", 0).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
	}

	// ------------------------------------------------------------ Concurrent changes

	@Test
	void outdatedStateVersionIsRejected() throws Exception {
		UUID matchId = createMatch();
		changeStatus(matchId, "READY", 0).andExpect(status().isOk());

		// A second organizer still sees version 0 and wants to cancel the match
		changeStatus(matchId, "CANCELLED", 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("STATE_VERSION_CONFLICT"));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.status").value("READY"))
			.andExpect(jsonPath("$.stateVersion").value(1));
	}

	@Test
	void simultaneousChangesLetOnlyOneThrough() throws Exception {
		UUID matchId = moveToWarmup(createMatch());
		int requests = 8;
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<MvcResult>> calls = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			String status = i % 2 == 0 ? "IN_PROGRESS" : "CALLED";
			calls.add(() -> {
				start.await();
				return changeStatus(matchId, status, 3).andReturn();
			});
		}

		List<MvcResult> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
			List<Future<MvcResult>> futures = new ArrayList<>();
			for (Callable<MvcResult> call : calls) {
				futures.add(executor.submit(call));
			}
			start.countDown();
			for (Future<MvcResult> future : futures) {
				results.add(future.get());
			}
		}

		List<Integer> statuses = results.stream().map(result -> result.getResponse().getStatus()).toList();
		assertThat(statuses).filteredOn(code -> code == 200).hasSize(1);
		assertThat(statuses).filteredOn(code -> code == 409).hasSize(requests - 1);
		for (MvcResult result : results) {
			if (result.getResponse().getStatus() == 409) {
				assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.code"))
					.isEqualTo("STATE_VERSION_CONFLICT");
			}
		}
		mvc.perform(get("/api/matches/{id}", matchId)).andExpect(jsonPath("$.stateVersion").value(4));
	}

	// ------------------------------------------------------------ Cascade

	@Test
	void deletingTheEventRemovesItsMatches() throws Exception {
		createMatch();

		jdbc.update("DELETE FROM event WHERE id = ?", eventId);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match", Integer.class)).isZero();
	}

	// ------------------------------------------------------------ Helpers

	private UUID createMatch() throws Exception {
		return createMatch(eventId, 1);
	}

	private UUID createMatch(UUID event, Integer round) throws Exception {
		return jsonId(create(event, round, UUID.randomUUID(), UUID.randomUUID()).andExpect(status().isCreated()));
	}

	private ResultActions create(UUID event, Integer round, UUID sideA, UUID sideB) throws Exception {
		String body = "{\"round\": %s, \"sideA\": %s, \"sideB\": %s}".formatted(round, quoted(sideA), quoted(sideB));
		return mvc.perform(post("/api/events/{id}/matches", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private UUID moveToWarmup(UUID matchId) throws Exception {
		changeStatus(matchId, "READY", 0).andExpect(status().isOk());
		callTo(matchId, addCourt(), 1).andExpect(status().isOk());
		changeStatus(matchId, "WARMUP", 2).andExpect(status().isOk());
		return matchId;
	}

	private UUID moveToInProgress(UUID matchId) throws Exception {
		moveToWarmup(matchId);
		changeStatus(matchId, "IN_PROGRESS", 3).andExpect(status().isOk());
		return matchId;
	}

	private ResultActions changeStatus(UUID matchId, String status, int stateVersion) throws Exception {
		return changeStatus(matchId, status, stateVersion, null, null);
	}

	private ResultActions changeStatus(UUID matchId, String status, int stateVersion, String winner, String outcome)
			throws Exception {
		String body = "{\"status\": \"%s\", \"stateVersion\": %d, \"winner\": %s, \"outcome\": %s}".formatted(status,
				stateVersion, quoted(winner), quoted(outcome));
		return mvc.perform(post("/api/matches/{id}/status", matchId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions callTo(UUID matchId, UUID court, int stateVersion) throws Exception {
		return mvc.perform(post("/api/matches/{id}/court", matchId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courtId\": \"%s\", \"stateVersion\": %d}".formatted(court, stateVersion)));
	}

	private UUID addCourt() throws Exception {
		return jsonId(mvc.perform(post("/api/tournaments/{id}/courts", tournamentId).with(organizer(jwtService)))
			.andExpect(status().isCreated()));
	}

	private String getMatch(UUID matchId) throws Exception {
		return mvc.perform(get("/api/matches/{id}", matchId)).andReturn().getResponse().getContentAsString();
	}

	private static UUID jsonId(ResultActions result) throws Exception {
		return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private static String quoted(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private UUID createEvent(UUID tournament, String name) throws Exception {
		String body = mvc.perform(post("/api/tournaments/{id}/events", tournament).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"%s\", \"discipline\": \"MS\", \"categoryType\": \"OPEN\"}".formatted(name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	private UUID insertTournament(String name) {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("""
				INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
				    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
				    created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, 'INDIVIDUAL', 'SINGLE_ELIMINATION', 'SETS', 2, 21, TRUE, 30, ?, ?)
				""", id, organizationId, name, Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), 20, Timestamp.from(now), Timestamp.from(now));
		jdbc.update("INSERT INTO tournament_discipline (tournament_id, sort_order, discipline) VALUES (?, 0, 'MS')", id);
		return id;
	}

}
