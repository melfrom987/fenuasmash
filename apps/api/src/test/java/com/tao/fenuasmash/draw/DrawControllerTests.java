package com.tao.fenuasmash.draw;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;

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

/** The tournament of {@link RegistrationTestSupport} is a single elimination. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DrawControllerTests extends RegistrationTestSupport {

	@Test
	void drawsTheConfirmedEntriesWithSeedsAndByes() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, teva, null);
		registered(event, hina, null);
		registered(event, moana, null);

		// Teva has the most points: seed 1, on line 1, facing the bye
		draw(event, "{}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.eventId").value(event.toString()))
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.pools", hasSize(0)))
			.andExpect(jsonPath("$.bracket.size").value(4))
			.andExpect(jsonPath("$.bracket.slots", hasSize(4)))
			.andExpect(jsonPath("$.bracket.slots[0].entry.player.id").value(teva.toString()))
			.andExpect(jsonPath("$.bracket.slots[0].entry.seed").value(1))
			.andExpect(jsonPath("$.bracket.slots[1].bye").value(true))
			.andExpect(jsonPath("$.randomSeed").isNumber())
			.andExpect(jsonPath("$.publishedAt").value(nullValue()));
	}

	@Test
	void leavesOutRegistrationsThatAreNotConfirmed() throws Exception {
		UUID event = createEvent("Double Mixte", "XD");
		registered(event, teva, hina);
		registered(event, manu, moana);
		UUID looking = registered(event, createdMan(), null);

		draw(event, "{}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.bracket.size").value(2))
			.andExpect(jsonPath("$.excludedRegistrationIds", hasSize(1)))
			.andExpect(jsonPath("$.excludedRegistrationIds[0]").value(looking.toString()));
	}

	@Test
	void givesTheSameDrawForTheSameRandomSeed() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		for (UUID player : new UUID[] { teva, manu, hina, moana }) {
			registered(event, player, null);
		}

		String first = body(draw(event, "{\"randomSeed\": 42}").andExpect(status().isCreated()));
		String again = body(draw(event, "{\"randomSeed\": 42}").andExpect(status().isCreated()));

		Object slots = JsonPath.read(first, "$.bracket.slots");
		org.assertj.core.api.Assertions.assertThat(JsonPath.<Object>read(again, "$.bracket.slots")).isEqualTo(slots);
		org.assertj.core.api.Assertions.assertThat(JsonPath.<Number>read(again, "$.randomSeed").longValue())
			.isEqualTo(42);
	}

	@Test
	void refusesTooFewEntries() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");
		registered(event, teva, null);

		draw(event, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ENOUGH_ENTRIES"));
		draw(event, "{\"randomSeed\": -1}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void isReadByOrganizersAndClubsOnly() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");

		mvc.perform(get("/api/events/{id}/draw", event).with(club(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DRAW_NOT_FOUND"));
		mvc.perform(get("/api/events/{id}/draw", event)).andExpect(status().isUnauthorized());
	}

	@Test
	void swapsTwoBracketLinesButNotALineWithAPoolSlot() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, teva, null);
		registered(event, hina, null);
		draw(event, "{}").andExpect(status().isCreated());

		swap(event, "{\"a\": {\"position\": 1}, \"b\": {\"position\": 2}}").andExpect(status().isOk())
			.andExpect(jsonPath("$.bracket.slots[1].entry.player.id").value(teva.toString()))
			.andExpect(jsonPath("$.bracket.slots[1].entry.seed").value(1));
		swap(event, "{\"a\": {\"position\": 1}, \"b\": {\"position\": 3}}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SWAP"));
		swap(event, "{\"a\": {\"position\": 1}, \"b\": {\"pool\": \"A\", \"index\": 0}}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SWAP"));
	}

	@Test
	void deletesTheDraftWhenTheRegistrationsChange() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, teva, null);
		registered(event, hina, null);
		draw(event, "{}").andExpect(status().isCreated());

		registered(event, moana, null);

		mvc.perform(get("/api/events/{id}/draw", event).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DRAW_NOT_FOUND"));
	}

	@Test
	void publishingLocksTheDrawAndTheRegistrations() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		UUID first = registered(event, teva, null);
		registered(event, hina, null);
		UUID other = createEvent("Double Open Série 2", "OD");
		UUID elsewhere = registered(other, moana, null);
		draw(event, "{}").andExpect(status().isCreated());

		mvc.perform(post("/api/events/{id}/draw/publish", event).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.publishedAt").isNotEmpty());

		for (ResultActions refused : new ResultActions[] { draw(event, "{}"),
				swap(event, "{\"a\": {\"position\": 1}, \"b\": {\"position\": 2}}"),
				mvc.perform(delete("/api/events/{id}/draw", event).with(organizer(jwtService))),
				mvc.perform(post("/api/events/{id}/draw/publish", event).with(organizer(jwtService))),
				register(event, moana, null), update(first, "{\"partnerId\": \"" + manu + "\"}"),
				mvc.perform(delete("/api/registrations/{id}", first).with(organizer(jwtService))),
				transfer(event, java.util.List.of(elsewhere)) }) {
			refused.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DRAW_PUBLISHED"));
		}
		transfer(other, java.util.List.of(first)).andExpect(status().isOk())
			.andExpect(jsonPath("$.rejected[0].code").value("DRAW_PUBLISHED"));
	}

	@Test
	void deletesADraft() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, teva, null);
		registered(event, hina, null);
		draw(event, "{}").andExpect(status().isCreated());

		mvc.perform(delete("/api/events/{id}/draw", event).with(organizer(jwtService)))
			.andExpect(status().isNoContent());
		mvc.perform(delete("/api/events/{id}/draw", event).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("DRAW_NOT_FOUND"));
	}

	private ResultActions draw(UUID eventId, String settings) throws Exception {
		return mvc.perform(post("/api/events/{id}/draw", eventId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(settings));
	}

	private ResultActions swap(UUID eventId, String json) throws Exception {
		return mvc.perform(post("/api/events/{id}/draw/swap", eventId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private UUID createdMan() throws Exception {
		String club = JsonPath.read(body(mvc.perform(get("/api/clubs"))), "$[0].id");
		return idOf(mvc.perform(post("/api/players").with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Tama", "lastName": "Roa", "gender": "MALE", "birthDate": "1990-01-01",
					 "clubId": "%s"}""".formatted(club))).andExpect(status().isCreated()));
	}

	private static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString();
	}

}
