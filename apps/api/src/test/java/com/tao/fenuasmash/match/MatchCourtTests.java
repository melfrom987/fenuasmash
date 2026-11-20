package com.tao.fenuasmash.match;

import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.common.security.JwtService;

/**
 * Matches on courts: the queue, calls to a court, one match per court, a side
 * never on two courts, and what the umpire tablet of a court may do.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MatchCourtTests {

	private static final UUID ALICE = UUID.randomUUID();

	private static final UUID BOB = UUID.randomUUID();

	private static final UUID CAROL = UUID.randomUUID();

	private static final UUID DAVE = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	private UUID tournamentId;

	private UUID eventId;

	private UUID court1;

	private UUID court2;

	@BeforeEach
	void setUp() throws Exception {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
		tournamentId = insertTournament("Open de Papeete");
		eventId = createEvent(tournamentId, "Simple Hommes");
		court1 = addCourt(tournamentId);
		court2 = addCourt(tournamentId);
	}

	// ------------------------------------------------------------ Calls to a court

	@Test
	void readyMatchIsCalledToACourt() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);

		callTo(matchId, court1, 1).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CALLED"))
			.andExpect(jsonPath("$.courtId").value(court1.toString()))
			.andExpect(jsonPath("$.stateVersion").value(2));
	}

	@Test
	void calledMatchCanMoveToAnotherCourt() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());

		callTo(matchId, court2, 2).andExpect(status().isOk())
			.andExpect(jsonPath("$.courtId").value(court2.toString()))
			.andExpect(jsonPath("$.stateVersion").value(3));
		// The first court is free again
		callTo(readyMatch(CAROL, DAVE), court1, 1).andExpect(status().isOk());
	}

	@Test
	void callingToTheSameCourtAgainChangesNothing() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());

		callTo(matchId, court1, 2).andExpect(status().isOk()).andExpect(jsonPath("$.stateVersion").value(2));
	}

	@Test
	void onlyReadyOrCalledMatchesAreCalled() throws Exception {
		UUID scheduled = createMatch(ALICE, BOB);
		callTo(scheduled, court1, 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

		UUID warmup = readyMatch(CAROL, DAVE);
		callTo(warmup, court1, 1).andExpect(status().isOk());
		changeStatus(warmup, "WARMUP", 2).andExpect(status().isOk());
		callTo(warmup, court2, 3).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void statusChangeCannotCallAMatchWithoutCourt() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);

		changeStatus(matchId, "CALLED", 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_REQUIRED"));
	}

	@Test
	void courtMustExistBelongToTheTournamentAndBeActive() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);

		callTo(matchId, UUID.randomUUID(), 1).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURT_NOT_FOUND"));
		callTo(matchId, addCourt(insertTournament("Open de Moorea")), 1).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_COURT"));
		mvc.perform(patch("/api/courts/{id}", court2).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"active\": false}")).andExpect(status().isOk());
		callTo(matchId, court2, 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_INACTIVE"));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.status").value("READY"))
			.andExpect(jsonPath("$.stateVersion").value(1));
	}

	@Test
	void callValidatesTheRequest() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);

		asOrganizer(matchId, "/court", "{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		callTo(UUID.randomUUID(), court1, 0).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
		callTo(matchId, court1, 0).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("STATE_VERSION_CONFLICT"));
	}

	@Test
	void onlyOrganizersCallMatches() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);
		String body = "{\"courtId\": \"%s\", \"stateVersion\": 1}".formatted(court1);
		String tablet = pairTablet(court1);

		mvc.perform(post("/api/matches/{id}/court", matchId).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/matches/{id}/court", matchId).with(club(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isForbidden());
		mvc.perform(asTablet(post("/api/matches/{id}/court", matchId), tablet).content(body))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// ------------------------------------------------------------ One match per court

	@Test
	void courtTakesOneMatchAtATime() throws Exception {
		UUID first = readyMatch(ALICE, BOB);
		UUID second = readyMatch(CAROL, DAVE);
		callTo(first, court1, 1).andExpect(status().isOk());

		callTo(second, court1, 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_OCCUPIED"));
		changeStatus(first, "WARMUP", 2).andExpect(status().isOk());
		changeStatus(first, "IN_PROGRESS", 3).andExpect(status().isOk());
		callTo(second, court1, 1).andExpect(jsonPath("$.code").value("COURT_OCCUPIED"));

		changeStatus(first, "FINISHED", 4, "A", "NORMAL").andExpect(status().isOk())
			.andExpect(jsonPath("$.courtId").value(court1.toString()));
		callTo(second, court1, 1).andExpect(status().isOk());
	}

	@Test
	void sendingAMatchBackToTheQueueFreesItsCourt() throws Exception {
		UUID first = readyMatch(ALICE, BOB);
		callTo(first, court1, 1).andExpect(status().isOk());

		changeStatus(first, "READY", 2).andExpect(status().isOk()).andExpect(jsonPath("$.courtId").isEmpty());
		callTo(readyMatch(CAROL, DAVE), court1, 1).andExpect(status().isOk());
	}

	@Test
	void cancelledOrWalkoverMatchKeepsItsCourtButFreesIt() throws Exception {
		UUID cancelled = readyMatch(ALICE, BOB);
		callTo(cancelled, court1, 1).andExpect(status().isOk());
		changeStatus(cancelled, "CANCELLED", 2).andExpect(jsonPath("$.courtId").value(court1.toString()));
		UUID walkover = readyMatch(CAROL, DAVE);
		callTo(walkover, court1, 1).andExpect(status().isOk());
		changeStatus(walkover, "WALKOVER", 2, "A", null).andExpect(jsonPath("$.courtId").value(court1.toString()));

		callTo(readyMatch(ALICE, DAVE), court1, 1).andExpect(status().isOk());
	}

	@Test
	void reopenedMatchNeedsItsCourtFree() throws Exception {
		UUID first = inProgressOn(court1, ALICE, BOB);
		changeStatus(first, "FINISHED", 4, "A", "NORMAL").andExpect(status().isOk());
		UUID second = readyMatch(CAROL, DAVE);
		callTo(second, court1, 1).andExpect(status().isOk());

		changeStatus(first, "IN_PROGRESS", 5).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_OCCUPIED"));

		changeStatus(second, "READY", 2).andExpect(status().isOk());
		changeStatus(first, "IN_PROGRESS", 5).andExpect(status().isOk())
			.andExpect(jsonPath("$.courtId").value(court1.toString()));
	}

	@Test
	void simultaneousCallsToOneCourtLetOnlyOneThrough() throws Exception {
		List<UUID> matches = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			matches.add(readyMatch(UUID.randomUUID(), UUID.randomUUID()));
		}

		List<MvcResult> results = runTogether(matches.stream().map(match -> callTo(match, court1)).toList());

		expectOneSuccessAndOthers(results, "COURT_OCCUPIED");
		assertThat(countOnCourt()).isEqualTo(1);
	}

	// ------------------------------------------------------------ A side on one court only

	@Test
	void sideIsNeverOnTwoCourts() throws Exception {
		UUID first = readyMatch(ALICE, BOB);
		callTo(first, court1, 1).andExpect(status().isOk());
		// Same registration in another match, even of another event of the tournament
		UUID second = readyMatch(CAROL, ALICE);
		UUID otherEvent = readyMatch(createEvent(tournamentId, "Simple Dames"), BOB, DAVE);

		callTo(second, court2, 1).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SIDE_ON_ANOTHER_COURT"));
		callTo(otherEvent, court2, 1).andExpect(jsonPath("$.code").value("SIDE_ON_ANOTHER_COURT"));

		changeStatus(first, "WARMUP", 2).andExpect(status().isOk());
		changeStatus(first, "IN_PROGRESS", 3).andExpect(status().isOk());
		changeStatus(first, "FINISHED", 4, "A", "NORMAL").andExpect(status().isOk());
		callTo(second, court2, 1).andExpect(status().isOk());
	}

	@Test
	void sideWaitingInTheQueueDoesNotBlockACall() throws Exception {
		readyMatch(ALICE, BOB);

		callTo(readyMatch(ALICE, CAROL), court1, 1).andExpect(status().isOk());
	}

	@Test
	void reopenedMatchNeedsItsSidesFree() throws Exception {
		UUID first = inProgressOn(court1, ALICE, BOB);
		changeStatus(first, "FINISHED", 4, "A", "NORMAL").andExpect(status().isOk());
		callTo(readyMatch(ALICE, CAROL), court2, 1).andExpect(status().isOk());

		changeStatus(first, "IN_PROGRESS", 5).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SIDE_ON_ANOTHER_COURT"));
	}

	@Test
	void simultaneousCallsOfOneSideLetOnlyOneThrough() throws Exception {
		List<UUID> courts = new ArrayList<>(List.of(court1, court2));
		for (int i = 0; i < 6; i++) {
			courts.add(addCourt(tournamentId));
		}
		List<Callable<MvcResult>> calls = new ArrayList<>();
		for (UUID court : courts) {
			calls.add(callTo(readyMatch(ALICE, UUID.randomUUID()), court));
		}

		List<MvcResult> results = runTogether(calls);

		expectOneSuccessAndOthers(results, "SIDE_ON_ANOTHER_COURT");
		assertThat(countOnCourt()).isEqualTo(1);
	}

	// ------------------------------------------------------------ Queue and courts

	@Test
	void queueListsReadyMatchesInOrderWithTheirAvailability() throws Exception {
		UUID onCourt = readyMatch(ALICE, BOB);
		callTo(onCourt, court1, 1).andExpect(status().isOk());
		UUID secondRound = readyMatch(eventId, 2, CAROL, DAVE);
		UUID blocked = readyMatch(BOB, CAROL);
		createMatch(DAVE, ALICE);

		mvc.perform(get("/api/tournaments/{id}/match-queue", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].match.id").value(blocked.toString()))
			.andExpect(jsonPath("$[0].available").value(false))
			.andExpect(jsonPath("$[1].match.id").value(secondRound.toString()))
			.andExpect(jsonPath("$[1].available").value(true));
	}

	@Test
	void courtAssignmentsShowEveryCourtWithItsMatch() throws Exception {
		UUID called = readyMatch(ALICE, BOB);
		callTo(called, court2, 1).andExpect(status().isOk());
		UUID finished = inProgressOn(court1, CAROL, DAVE);
		changeStatus(finished, "FINISHED", 4, "B", "NORMAL").andExpect(status().isOk());

		mvc.perform(get("/api/tournaments/{id}/court-assignments", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].court.id").value(court1.toString()))
			.andExpect(jsonPath("$[0].match").isEmpty())
			.andExpect(jsonPath("$[1].court.number").value(2))
			.andExpect(jsonPath("$[1].match.id").value(called.toString()));
	}

	@Test
	void queueAndCourtsReturnNotFoundForUnknownTournament() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/match-queue", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
		mvc.perform(get("/api/tournaments/{id}/court-assignments", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void courtThatReceivedAMatchCannotBeDeleted() throws Exception {
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court2, 1).andExpect(status().isOk());
		changeStatus(matchId, "CANCELLED", 2).andExpect(status().isOk());

		mvc.perform(delete("/api/courts/{id}", court2).with(organizer(jwtService)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_IN_USE"));
		mvc.perform(get("/api/courts/{id}", court2)).andExpect(status().isOk());
	}

	@Test
	void deletingTheTournamentRemovesCourtsAndMatches() throws Exception {
		callTo(readyMatch(ALICE, BOB), court1, 1).andExpect(status().isOk());

		jdbc.update("DELETE FROM tournament WHERE id = ?", tournamentId);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM court", Integer.class)).isZero();
	}

	// ------------------------------------------------------------ Umpire tablet

	@Test
	void tabletReadsTheMatchOnItsCourt() throws Exception {
		String tablet = pairTablet(court1);
		mvc.perform(asTablet(get("/api/tablets/me/match"), tablet)).andExpect(status().isNoContent());

		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());
		callTo(readyMatch(CAROL, DAVE), court2, 1).andExpect(status().isOk());

		mvc.perform(asTablet(get("/api/tablets/me/match"), tablet))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(matchId.toString()))
			.andExpect(jsonPath("$.status").value("CALLED"));
	}

	@Test
	void tabletMatchIsForTabletsOnly() throws Exception {
		mvc.perform(get("/api/tablets/me/match")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/tablets/me/match").with(organizer(jwtService))).andExpect(status().isForbidden());
	}

	@Test
	void tabletPlaysTheMatchOnItsCourt() throws Exception {
		String tablet = pairTablet(court1);
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());

		tabletStatus(tablet, matchId, "WARMUP", 2, null, null).andExpect(status().isOk());
		tabletStatus(tablet, matchId, "CALLED", 3, null, null).andExpect(status().isOk());
		tabletStatus(tablet, matchId, "WARMUP", 4, null, null).andExpect(status().isOk());
		tabletStatus(tablet, matchId, "IN_PROGRESS", 5, null, null).andExpect(status().isOk());
		tabletStatus(tablet, matchId, "FINISHED", 6, "B", "RETIRED").andExpect(status().isOk())
			.andExpect(jsonPath("$.outcome").value("RETIRED"));
		// Reopened to undo the last point
		tabletStatus(tablet, matchId, "IN_PROGRESS", 7, null, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.stateVersion").value(8));
	}

	@Test
	void tabletOnlyMovesTheMatchOfItsCourt() throws Exception {
		String tablet = pairTablet(court1);
		UUID elsewhere = readyMatch(ALICE, BOB);
		callTo(elsewhere, court2, 1).andExpect(status().isOk());
		UUID inQueue = readyMatch(CAROL, DAVE);

		tabletStatus(tablet, elsewhere, "WARMUP", 2, null, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_ON_TABLET_COURT"));
		tabletStatus(tablet, inQueue, "SCHEDULED", 1, null, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_ON_TABLET_COURT"));

		mvc.perform(get("/api/matches/{id}", elsewhere)).andExpect(jsonPath("$.status").value("CALLED"));
	}

	@Test
	void tabletLeavesQueueCancellationsAndWalkoversToTheOrganizer() throws Exception {
		String tablet = pairTablet(court1);
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());

		tabletStatus(tablet, matchId, "READY", 2, null, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED_FOR_TABLET"));
		tabletStatus(tablet, matchId, "CANCELLED", 2, null, null)
			.andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED_FOR_TABLET"));
		tabletStatus(tablet, matchId, "WALKOVER", 2, "A", null)
			.andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED_FOR_TABLET"));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.status").value("CALLED"))
			.andExpect(jsonPath("$.stateVersion").value(2));
	}

	@Test
	void tabletFollowsTheSameRulesAsTheOrganizer() throws Exception {
		String tablet = pairTablet(court1);
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());

		tabletStatus(tablet, matchId, "WARMUP", 1, null, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("STATE_VERSION_CONFLICT"));
		tabletStatus(tablet, matchId, "FINISHED", 2, "A", "NORMAL").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void revokedTabletCannotMoveItsMatch() throws Exception {
		String tablet = pairTablet(court1);
		UUID matchId = readyMatch(ALICE, BOB);
		callTo(matchId, court1, 1).andExpect(status().isOk());
		pairTablet(court1);

		tabletStatus(tablet, matchId, "WARMUP", 2, null, null).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("TABLET_REVOKED"));
	}

	// ------------------------------------------------------------ Helpers

	private UUID createMatch(UUID sideA, UUID sideB) throws Exception {
		return createMatch(eventId, 1, sideA, sideB);
	}

	private UUID createMatch(UUID event, Integer round, UUID sideA, UUID sideB) throws Exception {
		String body = "{\"round\": %s, \"sideA\": \"%s\", \"sideB\": \"%s\"}".formatted(round, sideA, sideB);
		return jsonId(mvc.perform(post("/api/events/{id}/matches", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isCreated()));
	}

	private UUID readyMatch(UUID sideA, UUID sideB) throws Exception {
		return readyMatch(eventId, 1, sideA, sideB);
	}

	private UUID readyMatch(UUID event, UUID sideA, UUID sideB) throws Exception {
		return readyMatch(event, 1, sideA, sideB);
	}

	private UUID readyMatch(UUID event, Integer round, UUID sideA, UUID sideB) throws Exception {
		UUID matchId = createMatch(event, round, sideA, sideB);
		changeStatus(matchId, "READY", 0).andExpect(status().isOk());
		return matchId;
	}

	private UUID inProgressOn(UUID court, UUID sideA, UUID sideB) throws Exception {
		UUID matchId = readyMatch(sideA, sideB);
		callTo(matchId, court, 1).andExpect(status().isOk());
		changeStatus(matchId, "WARMUP", 2).andExpect(status().isOk());
		changeStatus(matchId, "IN_PROGRESS", 3).andExpect(status().isOk());
		return matchId;
	}

	private ResultActions callTo(UUID matchId, UUID court, int stateVersion) throws Exception {
		return asOrganizer(matchId, "/court", "{\"courtId\": \"%s\", \"stateVersion\": %d}".formatted(court, stateVersion));
	}

	/** A call of a READY match, to run at the same time as others. */
	private Callable<MvcResult> callTo(UUID matchId, UUID court) {
		return () -> callTo(matchId, court, 1).andReturn();
	}

	private ResultActions changeStatus(UUID matchId, String status, int stateVersion) throws Exception {
		return changeStatus(matchId, status, stateVersion, null, null);
	}

	private ResultActions changeStatus(UUID matchId, String status, int stateVersion, String winner, String outcome)
			throws Exception {
		return asOrganizer(matchId, "/status", statusBody(status, stateVersion, winner, outcome));
	}

	private ResultActions tabletStatus(String tablet, UUID matchId, String status, int stateVersion, String winner,
			String outcome) throws Exception {
		return mvc.perform(asTablet(post("/api/matches/{id}/status", matchId), tablet)
			.content(statusBody(status, stateVersion, winner, outcome)));
	}

	private static String statusBody(String status, int stateVersion, String winner, String outcome) {
		return "{\"status\": \"%s\", \"stateVersion\": %d, \"winner\": %s, \"outcome\": %s}".formatted(status,
				stateVersion, quoted(winner), quoted(outcome));
	}

	private ResultActions asOrganizer(UUID matchId, String action, String body) throws Exception {
		return mvc.perform(post("/api/matches/{id}" + action, matchId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static MockHttpServletRequestBuilder asTablet(MockHttpServletRequestBuilder request, String token) {
		return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
	}

	/** Pairs a new tablet with the court, as the umpire would, and returns its token. */
	private String pairTablet(UUID court) throws Exception {
		String code = JsonPath.read(mvc.perform(post("/api/courts/{id}/pairing-code", court).with(organizer(jwtService)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.code");
		return JsonPath.read(mvc.perform(post("/api/tablets/pair").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\": \"%s\"}".formatted(code)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.accessToken");
	}

	private static List<MvcResult> runTogether(List<Callable<MvcResult>> calls) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<MvcResult> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(calls.size())) {
			List<Future<MvcResult>> futures = new ArrayList<>();
			for (Callable<MvcResult> call : calls) {
				futures.add(executor.submit(() -> {
					start.await();
					return call.call();
				}));
			}
			start.countDown();
			for (Future<MvcResult> future : futures) {
				results.add(future.get());
			}
		}
		return results;
	}

	private static void expectOneSuccessAndOthers(List<MvcResult> results, String code) throws Exception {
		assertThat(results.stream().map(result -> result.getResponse().getStatus()))
			.containsOnlyOnce(200)
			.filteredOn(status -> status != 200)
			.containsOnly(409);
		for (MvcResult result : results) {
			if (result.getResponse().getStatus() == 409) {
				assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.code")).isEqualTo(code);
			}
		}
	}

	private int countOnCourt() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM match WHERE status = 'CALLED'", Integer.class);
	}

	private UUID addCourt(UUID tournament) throws Exception {
		return jsonId(mvc.perform(post("/api/tournaments/{id}/courts", tournament).with(organizer(jwtService)))
			.andExpect(status().isCreated()));
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
