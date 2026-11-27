package com.tao.fenuasmash.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.tao.fenuasmash.TestTokens.organizer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.registration.RegistrationTestSupport;

/**
 * The schedule routes (`getSchedule`, `generateSchedule`, `moveScheduledMatch`,
 * `publishSchedule`) and the match fields they rely on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ScheduleControllerTests extends RegistrationTestSupport {

	@Test
	void isNotGeneratedAtFirst() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/schedule", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("NOT_GENERATED"))
			.andExpect(jsonPath("$.events", hasSize(0)))
			.andExpect(jsonPath("$.warnings", hasSize(0)))
			.andExpect(jsonPath("$.generatedAt").value(nullValue()));
		insertCourt(1);
		post("/move", "{\"matchId\": \"%s\", \"plannedStartAt\": \"2026-12-05T08:00:00Z\", \"courtId\": \"%s\"}"
			.formatted(UUID.randomUUID(), UUID.randomUUID())).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SCHEDULE_NOT_GENERATED"));
		post("/publish", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SCHEDULE_NOT_GENERATED"));
	}

	@Test
	void needsMatchesAndAnActiveCourt() throws Exception {
		post("", null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_ACTIVE_COURT"));
		insertCourt(1);
		post("", null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_MATCHES_TO_SCHEDULE"));
	}

	@Test
	void numbersTheMatchesOfADrawAndTellsWhereTheirSidesComeFrom() throws Exception {
		UUID event = publishedKnockout();

		List<Map<String, Object>> matches = matches(event);
		assertThat(matches).extracting((match) -> match.get("number")).containsExactly(1, 2, 3);
		assertThat(matches).extracting((match) -> match.get("phase")).containsOnly("KNOCKOUT");
		Map<String, Object> fin = matches.get(2);
		assertThat(fin.get("sourceA")).isEqualTo(Map.of("kind", "WINNER", "matchId", matches.get(0).get("id")));
		assertThat(fin.get("sourceB")).isEqualTo(Map.of("kind", "WINNER", "matchId", matches.get(1).get("id")));
		assertThat(matches.get(0).get("sourceA")).isNull();
	}

	@Test
	void plansTheMatchesThenMovesAndPublishesThem() throws Exception {
		UUID court1 = insertCourt(1);
		UUID court2 = insertCourt(2);
		UUID event = publishedKnockout();

		post("", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.generatedAt").isNotEmpty())
			.andExpect(jsonPath("$.events", hasSize(1)))
			.andExpect(jsonPath("$.events[0].eventId").value(event.toString()))
			.andExpect(jsonPath("$.events[0].matchCount").value(3))
			.andExpect(jsonPath("$.firstStartAt").isNotEmpty());
		List<Map<String, Object>> matches = matches(event);
		Map<String, Object> semi1 = matches.get(0);
		Map<String, Object> semi2 = matches.get(1);
		assertThat(semi1.get("plannedStartAt")).isEqualTo(semi2.get("plannedStartAt"));
		assertThat(List.of(semi1.get("plannedCourtId"), semi2.get("plannedCourtId")))
			.containsExactlyInAnyOrder(court1.toString(), court2.toString());
		assertThat((String) matches.get(2).get("plannedStartAt")).isGreaterThan((String) semi1.get("plannedStartAt"));

		// Moving a semi-final onto the other one swaps them
		post("/move", move(semi1.get("id"), semi2.get("plannedStartAt"), semi2.get("plannedCourtId")))
			.andExpect(status().isOk());
		List<Map<String, Object>> moved = matches(event);
		assertThat(moved.get(0).get("plannedCourtId")).isEqualTo(semi2.get("plannedCourtId"));
		assertThat(moved.get(1).get("plannedCourtId")).isEqualTo(semi1.get("plannedCourtId"));

		post("/publish", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.publishedAt").isNotEmpty());
		mvc.perform(get("/api/tournaments/{id}/schedule", tournamentId))
			.andExpect(jsonPath("$.status").value("PUBLISHED"));
		// Computing again sets it back to a draft
		post("", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.publishedAt").value(nullValue()));
	}

	@Test
	void refusesToMoveAStartedMatchOrOntoAnInactiveCourt() throws Exception {
		UUID court1 = insertCourt(1);
		UUID court2 = insertCourt(2);
		UUID event = publishedKnockout();
		post("", null).andExpect(status().isOk());
		Map<String, Object> semi = matches(event).get(0);
		Map<String, Object> fin = matches(event).get(2);
		jdbc.update("UPDATE match SET status = 'IN_PROGRESS', court_id = ? WHERE id = ?", court1,
				UUID.fromString((String) semi.get("id")));

		post("/move", move(semi.get("id"), fin.get("plannedStartAt"), court1)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_ALREADY_STARTED"));
		post("/move", move(fin.get("id"), semi.get("plannedStartAt"), semi.get("plannedCourtId")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
		jdbc.update("UPDATE court SET active = FALSE WHERE id = ?", court2);
		post("/move", move(fin.get("id"), fin.get("plannedStartAt"), court2)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_INACTIVE"));
		post("/move", move(UUID.randomUUID(), fin.get("plannedStartAt"), court1)).andExpect(status().isNotFound());
	}

	@Test
	void keepsTheEventSettingsForTheNextComputations() throws Exception {
		insertCourt(1);
		UUID event = publishedKnockout();

		post("", null).andExpect(status().isOk());
		// The final is held back to the next day
		Instant nextDay = Instant.parse((String) matches(event).get(0).get("plannedStartAt")).plus(Duration.ofDays(1));
		post("", "{\"events\": [{\"eventId\": \"%s\", \"offsetSlots\": 2, \"stageStarts\": [{\"stage\": \"FINAL\", \"at\": \"%s\"}]}]}"
			.formatted(event, nextDay)).andExpect(status().isOk())
			.andExpect(jsonPath("$.events[0].offsetSlots").value(2))
			.andExpect(jsonPath("$.events[0].stageStarts[0].stage").value("FINAL"));
		Map<String, Object> fin = matches(event).get(2);
		assertThat(Instant.parse((String) fin.get("plannedStartAt"))).isEqualTo(nextDay);

		post("", null).andExpect(status().isOk()).andExpect(jsonPath("$.events[0].offsetSlots").value(2));
		post("", "{\"events\": [{\"eventId\": \"%s\", \"offsetSlots\": 0}]}".formatted(UUID.randomUUID()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		post("", "{\"events\": [{\"eventId\": \"%s\", \"offsetSlots\": 101}]}".formatted(event))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void callsTheQueueInScheduleOrder() throws Exception {
		insertCourt(1);
		UUID event = publishedKnockout();
		post("", null).andExpect(status().isOk());
		List<Map<String, Object>> matches = matches(event);
		for (Map<String, Object> match : matches.subList(0, 2)) {
			jdbc.update("UPDATE match SET status = 'READY' WHERE id = ?", UUID.fromString((String) match.get("id")));
		}
		// The second semi-final is planned first
		post("/move", move(matches.get(1).get("id"), "2026-11-01T07:00:00Z", matches.get(1).get("plannedCourtId")))
			.andExpect(status().isOk());

		String queue = mvc.perform(get("/api/tournaments/{id}/match-queue", tournamentId).with(organizer(jwtService)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(JsonPath.<List<String>>read(queue, "$[*].match.id")).containsExactly(
				(String) matches.get(1).get("id"), (String) matches.get(0).get("id"));
	}

	@Test
	void isComputedByOrganizersOnly() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.post("/api/tournaments/{id}/schedule", tournamentId)).andExpect(status().isUnauthorized());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.post("/api/tournaments/{id}/schedule/publish", tournamentId)).andExpect(status().isUnauthorized());
	}

	/** A single elimination of 4 entries, published: two semi-finals and a final. */
	private UUID publishedKnockout() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");
		registered(event, teva, null);
		registered(event, manu, null);
		registered(event, insertPlayer("Tama", "Roa", "MALE", LocalDate.of(1990, 1, 1), 500), null);
		registered(event, insertPlayer("Toa", "Faua", "MALE", LocalDate.of(1992, 1, 1), 100), null);
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.post("/api/events/{id}/draw", event)
			.with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isCreated());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.post("/api/events/{id}/draw/publish", event)
			.with(organizer(jwtService))).andExpect(status().isOk());
		return event;
	}

	private List<Map<String, Object>> matches(UUID event) throws Exception {
		String body = mvc.perform(get("/api/events/{id}/matches", event)).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$");
	}

	private ResultActions post(String path, String json) throws Exception {
		var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.post("/api/tournaments/{id}/schedule" + path, tournamentId)
			.with(organizer(jwtService));
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return mvc.perform(request);
	}

	private static String move(Object matchId, Object startAt, Object courtId) {
		return "{\"matchId\": \"%s\", \"plannedStartAt\": \"%s\", \"courtId\": \"%s\"}".formatted(matchId, startAt,
				courtId);
	}

	private UUID insertCourt(int number) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO court (id, tournament_id, number, active) VALUES (?, ?, ?, TRUE)", id, tournamentId,
				number);
		return id;
	}

}
