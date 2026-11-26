package com.tao.fenuasmash.match;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.organizer;

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

/** The winner of a knockout match takes its side in the next round (#96). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KnockoutAdvancementTests extends RegistrationTestSupport {

	@Test
	void sendsTheWinnersToTheFinalAndPlaysIt() throws Exception {
		UUID event = publishedKnockout(teva, manu, man("Tama", 500), man("Toa", 100));
		Map<String, Object> top = match(event, 1, 1);
		Map<String, Object> bottom = match(event, 1, 2);

		walkover(top, "B").andExpect(status().isOk());
		walkover(bottom, "A").andExpect(status().isOk());

		Map<String, Object> fin = match(event, 2, 1);
		org.assertj.core.api.Assertions.assertThat(fin.get("sideA")).isEqualTo(top.get("sideB"));
		org.assertj.core.api.Assertions.assertThat(fin.get("sideB")).isEqualTo(bottom.get("sideA"));
		// The final has no next match
		walkover(fin, "A").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WALKOVER"));
	}

	@Test
	void joinsTheEntryThatHadABye() throws Exception {
		// Teva, seed 1, faces the bye and waits in the final
		UUID event = publishedKnockout(teva, manu, man("Tama", 500));
		Map<String, Object> semi = match(event, 1, 2);

		walkover(semi, "A").andExpect(status().isOk());

		Map<String, Object> fin = match(event, 2, 1);
		org.assertj.core.api.Assertions.assertThat(fin.get("sideA")).isNotNull();
		org.assertj.core.api.Assertions.assertThat(fin.get("sideB")).isEqualTo(semi.get("sideA"));
	}

	@Test
	void takesTheWinnerBackWhenTheMatchIsReopened() throws Exception {
		UUID event = publishedKnockout(teva, manu, man("Tama", 500), man("Toa", 100));
		Map<String, Object> top = match(event, 1, 1);
		UUID court = insertCourt(1);
		jdbc.update("UPDATE match SET status = 'IN_PROGRESS', court_id = ? WHERE id = ?", court,
				UUID.fromString((String) top.get("id")));
		top = match(event, 1, 1);

		change(top, "{\"status\": \"FINISHED\", \"winner\": \"A\", \"outcome\": \"NORMAL\"}")
			.andExpect(status().isOk());
		mvc.perform(get("/api/events/{id}/matches", event))
			.andExpect(jsonPath("$[2].sideA").value(top.get("sideA")));

		change(match(event, 1, 1), "{\"status\": \"IN_PROGRESS\"}").andExpect(status().isOk());
		mvc.perform(get("/api/events/{id}/matches", event)).andExpect(jsonPath("$[2].sideA").value(nullValue()));

		// Finished again with the other winner
		change(match(event, 1, 1), "{\"status\": \"FINISHED\", \"winner\": \"B\", \"outcome\": \"RETIRED\"}")
			.andExpect(status().isOk());
		mvc.perform(get("/api/events/{id}/matches", event))
			.andExpect(jsonPath("$[2].sideA").value(top.get("sideB")));
	}

	@Test
	void refusesToReopenAMatchOnceTheNextOneIsCalled() throws Exception {
		UUID event = publishedKnockout(teva, manu, man("Tama", 500), man("Toa", 100));
		Map<String, Object> top = match(event, 1, 1);
		UUID court = insertCourt(1);
		jdbc.update("UPDATE match SET status = 'IN_PROGRESS', court_id = ? WHERE id = ?", court,
				UUID.fromString((String) top.get("id")));
		change(match(event, 1, 1), "{\"status\": \"FINISHED\", \"winner\": \"A\", \"outcome\": \"NORMAL\"}")
			.andExpect(status().isOk());
		walkover(match(event, 1, 2), "A").andExpect(status().isOk());
		jdbc.update("UPDATE match SET status = 'CALLED', court_id = ? WHERE id = ?", insertCourt(2),
				UUID.fromString((String) match(event, 2, 1).get("id")));

		change(match(event, 1, 1), "{\"status\": \"IN_PROGRESS\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NEXT_MATCH_STARTED"));
	}

	private UUID publishedKnockout(UUID... players) throws Exception {
		UUID event = createEvent("Simple Homme", "MS");
		for (UUID player : players) {
			registered(event, player, null);
		}
		mvc.perform(post("/api/events/{id}/draw", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isCreated());
		mvc.perform(post("/api/events/{id}/draw/publish", event).with(organizer(jwtService)))
			.andExpect(status().isOk());
		return event;
	}

	private UUID man(String firstName, int points) {
		return insertPlayer(firstName, "Test", "MALE", LocalDate.of(1990, 1, 1), points);
	}

	private Map<String, Object> match(UUID event, int round, int position) throws Exception {
		String body = mvc.perform(get("/api/events/{id}/matches", event))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<Map<String, Object>> found = JsonPath.read(body,
				"$[?(@.round == " + round + " && @.bracketPosition == " + position + ")]");
		return found.getFirst();
	}

	private ResultActions walkover(Map<String, Object> match, String winner) throws Exception {
		return change(match, "{\"status\": \"WALKOVER\", \"winner\": \"" + winner + "\"}");
	}

	private ResultActions change(Map<String, Object> match, String json) throws Exception {
		String body = json.substring(0, json.length() - 1) + ", \"stateVersion\": " + match.get("stateVersion") + "}";
		return mvc.perform(post("/api/matches/{id}/status", match.get("id")).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private UUID insertCourt(int number) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO court (id, tournament_id, number, active) VALUES (?, ?, ?, TRUE)", id, tournamentId,
				number);
		return id;
	}

}
