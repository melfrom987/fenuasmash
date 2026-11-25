package com.tao.fenuasmash.match;

import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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

/**
 * Points sent by the umpire tablet: counted with the rules of the tournament,
 * undone, never counted twice, applied in sequence order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ScoreEventControllerTests {

	private static final Instant T0 = Instant.parse("2026-11-21T18:00:00.123456Z");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID organizationId;

	private UUID tournamentId;

	private UUID eventId;

	private UUID court;

	private UUID matchId;

	private String tablet;

	@BeforeEach
	void setUp() throws Exception {
		DatabaseCleaner.clean(jdbc);
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
		tournamentId = insertTournament();
		eventId = createEvent();
		court = addCourt();
		tablet = pairTablet(court);
		matchId = matchInProgressOn(court);
	}

	// ------------------------------------------------------------ Points

	@Test
	void pointsAreCountedAndConfirmed() throws Exception {
		List<Event> events = List.of(point(1, "A"), point(2, "B"), point(3, "A"));

		send(events).andExpect(status().isOk())
			.andExpect(jsonPath("$.acceptedIds", contains(ids(events))))
			.andExpect(jsonPath("$.duplicateIds", empty()))
			.andExpect(jsonPath("$.lastSequence").value(3))
			.andExpect(jsonPath("$.score.games[0].a").value(2))
			.andExpect(jsonPath("$.score.games[0].b").value(1))
			.andExpect(jsonPath("$.score.winner").isEmpty())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.stateVersion").value(5));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.score.games", hasSize(1)))
			.andExpect(jsonPath("$.score.games[0].a").value(2))
			.andExpect(jsonPath("$.stateVersion").value(5));
	}

	@Test
	void eachEventIsStoredWithItsTimesAndTablet() throws Exception {
		send(List.of(point(1, "A"))).andExpect(status().isOk());

		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM score_event");
		assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(T0.plusSeconds(1));
		assertThat(((Timestamp) row.get("received_at")).toInstant()).isCloseTo(Instant.now(),
				org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES));
		assertThat(row.get("tablet_id")).isNotNull();
		assertThat(row.get("side")).isEqualTo("A");
	}

	@Test
	void newMatchHasNoScore() throws Exception {
		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.score.games", empty()))
			.andExpect(jsonPath("$.score.winner").isEmpty());
	}

	// ------------------------------------------------------------ Undo

	@Test
	void undoCancelsTheLastPoint() throws Exception {
		send(List.of(point(1, "A"), point(2, "B"), undo(3))).andExpect(status().isOk())
			.andExpect(jsonPath("$.score.games[0].a").value(1))
			.andExpect(jsonPath("$.score.games[0].b").value(0));

		send(List.of(undo(4), undo(5))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SCORE_EVENT"));
		send(List.of(undo(4))).andExpect(status().isOk()).andExpect(jsonPath("$.score.games", empty()));
	}

	@Test
	void undoGoesBackToThePreviousGame() throws Exception {
		send(rallies(1, 21, 0)).andExpect(jsonPath("$.score.games", hasSize(1)));
		send(List.of(point(22, "B"))).andExpect(jsonPath("$.score.games", hasSize(2)));

		send(List.of(undo(23))).andExpect(jsonPath("$.score.games", hasSize(1)));
		send(List.of(undo(24))).andExpect(jsonPath("$.score.games[0].a").value(20));
	}

	// ------------------------------------------------------------ Duplicates

	@Test
	void eventsSentAgainAreConfirmedButNotCountedTwice() throws Exception {
		List<Event> events = List.of(point(1, "A"), point(2, "A"));
		send(events).andExpect(status().isOk());

		send(events).andExpect(status().isOk())
			.andExpect(jsonPath("$.acceptedIds", empty()))
			.andExpect(jsonPath("$.duplicateIds", contains(ids(events))))
			.andExpect(jsonPath("$.lastSequence").value(2))
			.andExpect(jsonPath("$.score.games[0].a").value(2))
			.andExpect(jsonPath("$.stateVersion").value(5));
		assertThat(countEvents()).isEqualTo(2);
	}

	@Test
	void lostAnswerIsRecoveredWithTheNextBatch() throws Exception {
		List<Event> first = List.of(point(1, "A"), point(2, "B"));
		send(first).andExpect(status().isOk());

		Event third = point(3, "B");
		send(List.of(first.get(0), first.get(1), third)).andExpect(status().isOk())
			.andExpect(jsonPath("$.acceptedIds", contains(third.id().toString())))
			.andExpect(jsonPath("$.duplicateIds", contains(ids(first))))
			.andExpect(jsonPath("$.score.games[0].b").value(2));
	}

	@Test
	void sameEventTwiceInABatchCountsOnce() throws Exception {
		Event event = point(1, "A");

		send(List.of(event, event)).andExpect(status().isOk())
			.andExpect(jsonPath("$.acceptedIds", contains(event.id().toString())))
			.andExpect(jsonPath("$.score.games[0].a").value(1));
	}

	@Test
	void sameIdWithAnotherContentIsRejected() throws Exception {
		Event event = point(1, "A");
		send(List.of(event)).andExpect(status().isOk());

		send(List.of(new Event(event.id(), 1, "POINT", "B", event.occurredAt()))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_ID_CONFLICT"));
		send(List.of(new Event(event.id(), 2, "POINT", "A", event.occurredAt())))
			.andExpect(jsonPath("$.code").value("EVENT_ID_CONFLICT"));
		Event repeated = point(2, "A");
		send(List.of(repeated, new Event(repeated.id(), 2, "POINT", "B", repeated.occurredAt())))
			.andExpect(jsonPath("$.code").value("EVENT_ID_CONFLICT"));
	}

	@Test
	void eventIdOfAnotherMatchIsRejected() throws Exception {
		Event event = point(1, "A");
		send(List.of(event)).andExpect(status().isOk());
		UUID otherMatch = matchInProgressOn(court, true);

		send(otherMatch, tablet, List.of(event)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_ID_CONFLICT"));
	}

	@Test
	void simultaneousCopiesOfABatchAreCountedOnce() throws Exception {
		List<Event> events = rallies(1, 5, 3);
		int requests = 8;
		CountDownLatch start = new CountDownLatch(1);
		List<MvcResult> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
			List<Future<MvcResult>> futures = new ArrayList<>();
			for (int i = 0; i < requests; i++) {
				futures.add(executor.submit(() -> {
					start.await();
					return send(events).andReturn();
				}));
			}
			start.countDown();
			for (Future<MvcResult> future : futures) {
				results.add(future.get());
			}
		}

		assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
		List<Integer> accepted = results.stream()
			.map(result -> JsonPath.<List<String>>read(content(result), "$.acceptedIds").size())
			.toList();
		assertThat(accepted).containsOnlyOnce(events.size()).filteredOn(size -> size != events.size()).containsOnly(0);
		assertThat(countEvents()).isEqualTo(events.size());
		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.score.games[0].a").value(5))
			.andExpect(jsonPath("$.score.games[0].b").value(3));
	}

	// ------------------------------------------------------------ Sequence

	@Test
	void eventsAreAppliedInSequenceOrder() throws Exception {
		Event first = point(1, "A");
		Event second = undo(2);
		Event third = point(3, "B");

		send(List.of(third, first, second)).andExpect(status().isOk())
			.andExpect(jsonPath("$.lastSequence").value(3))
			.andExpect(jsonPath("$.acceptedIds", contains(ids(List.of(first, second, third)))))
			.andExpect(jsonPath("$.score.games[0].a").value(0))
			.andExpect(jsonPath("$.score.games[0].b").value(1));
	}

	@Test
	void missingEventsAreAskedAgainAndNothingIsApplied() throws Exception {
		send(List.of(point(1, "A"), point(2, "A"))).andExpect(status().isOk());

		send(List.of(point(4, "B"), point(5, "B"))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SEQUENCE_GAP"))
			.andExpect(jsonPath("$.expectedSequence").value(3));
		send(List.of(point(3, "B"), point(5, "B"))).andExpect(jsonPath("$.code").value("SEQUENCE_GAP"))
			.andExpect(jsonPath("$.expectedSequence").value(4));

		assertThat(countEvents()).isEqualTo(2);
	}

	@Test
	void firstEventMustBeSequenceOne() throws Exception {
		send(List.of(point(2, "A"))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SEQUENCE_GAP"))
			.andExpect(jsonPath("$.expectedSequence").value(1));
	}

	@Test
	void sequenceUsedByAnotherEventIsRejected() throws Exception {
		send(List.of(point(1, "A"), point(2, "A"))).andExpect(status().isOk());

		send(List.of(point(2, "B"))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SEQUENCE_ALREADY_USED"));
		send(List.of(point(3, "B"), point(3, "A"))).andExpect(jsonPath("$.code").value("SEQUENCE_ALREADY_USED"));
		assertThat(countEvents()).isEqualTo(2);
	}

	@Test
	void batchIsAppliedWholeOrNotAtAll() throws Exception {
		send(List.of(point(1, "A"), undo(2), undo(3))).andExpect(status().isBadRequest());

		assertThat(countEvents()).isZero();
		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.score.games", empty()))
			.andExpect(jsonPath("$.stateVersion").value(4));
	}

	@Test
	void fullBatchOfAHundredEventsIsAccepted() throws Exception {
		List<Event> events = new ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			events.add(i % 2 == 0 ? undo(i) : point(i, "A"));
		}

		send(events).andExpect(status().isOk()).andExpect(jsonPath("$.lastSequence").value(100));
	}

	// ------------------------------------------------------------ End of the match

	@Test
	void winningPointFinishesTheMatchAndFreesTheCourt() throws Exception {
		send(rallies(1, 21, 15)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		send(rallies(37, 21, 19)).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.score.winner").value("A"))
			.andExpect(jsonPath("$.score.games", hasSize(2)));

		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.winner").value("A"))
			.andExpect(jsonPath("$.outcome").value("NORMAL"))
			.andExpect(jsonPath("$.endedAt").isNotEmpty());
		mvc.perform(get("/api/tournaments/{id}/court-assignments", tournamentId))
			.andExpect(jsonPath("$[0].match").isEmpty());
	}

	@Test
	void gameGoesOnUntilTwoPointsAheadOrTheCap() throws Exception {
		send(rallies(1, 20, 20)).andExpect(jsonPath("$.score.games", hasSize(1)));
		send(List.of(point(41, "A"))).andExpect(jsonPath("$.score.games", hasSize(1)));
		send(List.of(point(42, "B"), point(43, "A"), point(44, "A"))).andExpect(jsonPath("$.score.games[0].a").value(23))
			.andExpect(jsonPath("$.score.games[0].b").value(21));

		send(rallies(45, 29, 29)).andExpect(jsonPath("$.score.games", hasSize(2)));
		send(List.of(point(103, "B"))).andExpect(jsonPath("$.score.games[1].b").value(30))
			.andExpect(jsonPath("$.score.winner").isEmpty());
	}

	@Test
	void noPointAfterTheMatchIsWon() throws Exception {
		List<Event> match = winForA(1);
		Event extra = point(match.size() + 1, "B");

		List<Event> withExtra = new ArrayList<>(match);
		withExtra.add(extra);
		send(withExtra).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MATCH_NOT_IN_PROGRESS"));
		assertThat(countEvents()).isZero();

		send(match).andExpect(jsonPath("$.status").value("FINISHED"));
		send(List.of(extra)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_IN_PROGRESS"));
	}

	@Test
	void eventsOfAFinishedMatchAreStillConfirmedWhenSentAgain() throws Exception {
		List<Event> match = winForA(1);
		send(match).andExpect(status().isOk());

		send(match).andExpect(status().isOk())
			.andExpect(jsonPath("$.acceptedIds", empty()))
			.andExpect(jsonPath("$.duplicateIds", hasSize(match.size())))
			.andExpect(jsonPath("$.status").value("FINISHED"));
	}

	@Test
	void undoingTheWinningPointReopensTheMatch() throws Exception {
		List<Event> match = winForA(1);
		send(match).andExpect(status().isOk());

		send(List.of(undo(match.size() + 1))).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.score.winner").isEmpty())
			.andExpect(jsonPath("$.score.games[1].a").value(20));
		mvc.perform(get("/api/matches/{id}", matchId))
			.andExpect(jsonPath("$.winner").isEmpty())
			.andExpect(jsonPath("$.outcome").isEmpty())
			.andExpect(jsonPath("$.endedAt").isEmpty())
			.andExpect(jsonPath("$.courtId").value(court.toString()));
		send(List.of(point(match.size() + 2, "B"))).andExpect(status().isOk());
	}

	@Test
	void undoingTheWinningPointNeedsTheCourtFree() throws Exception {
		List<Event> match = winForA(1);
		send(match).andExpect(status().isOk());
		matchCalledTo(court);

		send(List.of(undo(match.size() + 1))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_OCCUPIED"));
		mvc.perform(get("/api/matches/{id}", matchId)).andExpect(jsonPath("$.status").value("FINISHED"));
	}

	@Test
	void retiredMatchTakesNoEvent() throws Exception {
		send(List.of(point(1, "A"))).andExpect(status().isOk());
		organizerStatus(matchId, "FINISHED", 5, "B", "RETIRED").andExpect(status().isOk());

		send(List.of(undo(2))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_IN_PROGRESS"));
	}

	@Test
	void matchNotStartedTakesNoPoint() throws Exception {
		UUID called = matchCalledTo(addCourt());
		String otherTablet = pairTablet(courtOf(called));

		send(called, otherTablet, List.of(point(1, "A"))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_IN_PROGRESS"));
	}

	// ------------------------------------------------------------ Access

	@Test
	void onlyTheTabletOfTheCourtSendsItsPoints() throws Exception {
		String otherTablet = pairTablet(addCourt());

		send(matchId, otherTablet, List.of(point(1, "A"))).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_ON_TABLET_COURT"));
		mvc.perform(post("/api/matches/{id}/events", matchId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(List.of(point(1, "A"))))).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mvc.perform(post("/api/matches/{id}/events", matchId).contentType(MediaType.APPLICATION_JSON)
			.content(batch(List.of(point(1, "A"))))).andExpect(status().isUnauthorized());
		assertThat(countEvents()).isZero();
	}

	@Test
	void revokedTabletCannotSendPoints() throws Exception {
		pairTablet(court);

		send(List.of(point(1, "A"))).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("TABLET_REVOKED"));
	}

	@Test
	void unknownMatchIsNotFound() throws Exception {
		send(UUID.randomUUID(), tablet, List.of(point(1, "A"))).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
	}

	// ------------------------------------------------------------ Validation

	@Test
	void batchIsValidated() throws Exception {
		sendRaw("{\"events\": []}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		sendRaw("{}").andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		sendRaw("{\"events\": [{\"sequence\": 1, \"type\": \"POINT\", \"side\": \"A\", \"occurredAt\": \"%s\"}]}"
			.formatted(T0)).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		sendRaw(batch(List.of(new Event(UUID.randomUUID(), 0, "POINT", "A", T0))))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		List<Event> tooMany = new ArrayList<>();
		for (int i = 1; i <= 101; i++) {
			tooMany.add(point(i, "A"));
		}
		send(tooMany).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void pointNeedsASideAndUndoHasNone() throws Exception {
		send(List.of(new Event(UUID.randomUUID(), 1, "POINT", null, T0))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SCORE_EVENT"));
		send(List.of(point(1, "A"), new Event(UUID.randomUUID(), 2, "UNDO", "A", T0)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SCORE_EVENT"));
		assertThat(countEvents()).isZero();
	}

	// ------------------------------------------------------------ Helpers

	private record Event(UUID id, int sequence, String type, String side, Instant occurredAt) {

		String json() {
			String sideMember = side == null ? "" : ", \"side\": \"" + side + "\"";
			return "{\"id\": \"%s\", \"sequence\": %d, \"type\": \"%s\"%s, \"occurredAt\": \"%s\"}".formatted(id,
					sequence, type, sideMember, occurredAt);
		}

	}

	private static Event point(int sequence, String side) {
		return new Event(UUID.randomUUID(), sequence, "POINT", side, T0.plusSeconds(sequence));
	}

	private static Event undo(int sequence) {
		return new Event(UUID.randomUUID(), sequence, "UNDO", null, T0.plusSeconds(sequence));
	}

	/**
	 * Points of one game from {@code firstSequence}, alternating up to the lower
	 * score, then the rest for the side ahead, so that the game ends on its last
	 * point only.
	 */
	private static List<Event> rallies(int firstSequence, int a, int b) {
		List<Event> events = new ArrayList<>();
		int sequence = firstSequence;
		int min = Math.min(a, b);
		for (int i = 0; i < min; i++) {
			events.add(point(sequence++, "A"));
			events.add(point(sequence++, "B"));
		}
		for (int i = min; i < a; i++) {
			events.add(point(sequence++, "A"));
		}
		for (int i = min; i < b; i++) {
			events.add(point(sequence++, "B"));
		}
		return events;
	}

	/** 21-15, 21-19 for side A, the last point being the winning one. */
	private static List<Event> winForA(int firstSequence) {
		List<Event> events = new ArrayList<>(rallies(firstSequence, 21, 15));
		events.addAll(rallies(firstSequence + events.size(), 21, 19));
		return events;
	}

	private static String[] ids(List<Event> events) {
		return events.stream().map(event -> event.id().toString()).toArray(String[]::new);
	}

	private static String batch(List<Event> events) {
		return "{\"events\": [" + events.stream().map(Event::json).collect(Collectors.joining(", ")) + "]}";
	}

	private ResultActions send(List<Event> events) throws Exception {
		return send(matchId, tablet, events);
	}

	private ResultActions send(UUID match, String token, List<Event> events) throws Exception {
		return mvc.perform(post("/api/matches/{id}/events", match).header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(events)));
	}

	private ResultActions sendRaw(String body) throws Exception {
		return mvc.perform(post("/api/matches/{id}/events", matchId).header("Authorization", "Bearer " + tablet)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private int countEvents() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM score_event", Integer.class);
	}

	private static String content(MvcResult result) {
		try {
			return result.getResponse().getContentAsString();
		}
		catch (java.io.UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private UUID matchInProgressOn(UUID courtId) throws Exception {
		return matchInProgressOn(courtId, false);
	}

	/**
	 * A match in progress on the court, at state version 4.
	 *
	 * @param freeCourtFirst finish the match on the court first, so that it is free
	 */
	private UUID matchInProgressOn(UUID courtId, boolean freeCourtFirst) throws Exception {
		if (freeCourtFirst) {
			int version = JsonPath.read(content(mvc.perform(get("/api/matches/{id}", matchId)).andReturn()),
					"$.stateVersion");
			organizerStatus(matchId, "FINISHED", version, "A", "RETIRED").andExpect(status().isOk());
		}
		UUID id = matchCalledTo(courtId);
		organizerStatus(id, "WARMUP", 2, null, null).andExpect(status().isOk());
		organizerStatus(id, "IN_PROGRESS", 3, null, null).andExpect(status().isOk());
		return id;
	}

	/** A match called to the court, at state version 2. */
	private UUID matchCalledTo(UUID courtId) throws Exception {
		String body = "{\"round\": 1, \"sideA\": \"%s\", \"sideB\": \"%s\"}".formatted(UUID.randomUUID(),
				UUID.randomUUID());
		UUID id = jsonId(mvc.perform(post("/api/events/{id}/matches", eventId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isCreated()));
		organizerStatus(id, "READY", 0, null, null).andExpect(status().isOk());
		mvc.perform(post("/api/matches/{id}/court", id).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courtId\": \"%s\", \"stateVersion\": 1}".formatted(courtId))).andExpect(status().isOk());
		return id;
	}

	private UUID courtOf(UUID match) throws Exception {
		return UUID.fromString(JsonPath.read(content(mvc.perform(get("/api/matches/{id}", match)).andReturn()),
				"$.courtId"));
	}

	private ResultActions organizerStatus(UUID match, String status, int stateVersion, String winner, String outcome)
			throws Exception {
		String body = "{\"status\": \"%s\", \"stateVersion\": %d, \"winner\": %s, \"outcome\": %s}".formatted(status,
				stateVersion, quoted(winner), quoted(outcome));
		return mvc.perform(post("/api/matches/{id}/status", match).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String quoted(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private String pairTablet(UUID courtId) throws Exception {
		String code = JsonPath.read(content(mvc.perform(post("/api/courts/{id}/pairing-code", courtId)
			.with(organizer(jwtService))).andExpect(status().isCreated()).andReturn()), "$.code");
		return JsonPath.read(content(mvc.perform(post("/api/tablets/pair").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\": \"%s\"}".formatted(code))).andExpect(status().isOk()).andReturn()), "$.accessToken");
	}

	private UUID addCourt() throws Exception {
		return jsonId(mvc.perform(post("/api/tournaments/{id}/courts", tournamentId).with(organizer(jwtService)))
			.andExpect(status().isCreated()));
	}

	private static UUID jsonId(ResultActions result) throws Exception {
		return UUID.fromString(JsonPath.read(content(result.andReturn()), "$.id"));
	}

	private UUID createEvent() throws Exception {
		return jsonId(mvc.perform(post("/api/tournaments/{id}/events", tournamentId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"Simple Hommes\", \"discipline\": \"MS\", \"categoryType\": \"OPEN\"}"))
			.andExpect(status().isCreated()));
	}

	/** 2 games of 21 points, 2 ahead, 30 at most. */
	private UUID insertTournament() {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("""
				INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
				    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
				    created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, 'INDIVIDUAL', 'SINGLE_ELIMINATION', 'SETS', 2, 21, TRUE, 30, ?, ?)
				""", id, organizationId, "Open de Papeete", Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), 20, Timestamp.from(now), Timestamp.from(now));
		jdbc.update("INSERT INTO tournament_discipline (tournament_id, sort_order, discipline) VALUES (?, 0, 'MS')", id);
		return id;
	}

}
