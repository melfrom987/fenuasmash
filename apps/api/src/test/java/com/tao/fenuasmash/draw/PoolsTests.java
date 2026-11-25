package com.tao.fenuasmash.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.organizer;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.registration.RegistrationTestSupport;

/** Pools then knockout (#93): pool matches, standings, and qualifiers into the bracket. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PoolsTests extends RegistrationTestSupport {

	private UUID event;

	@BeforeEach
	void publishTwoPoolsOfThree() throws Exception {
		jdbc.update("UPDATE tournament SET draw_format = 'POOLS_THEN_ELIMINATION' WHERE id = ?", tournamentId);
		event = createEvent("Simple Homme", "MS");
		registered(event, teva, null);
		registered(event, manu, null);
		for (int i = 0; i < 4; i++) {
			registered(event, insertPlayer("Joueur" + i, "Test", "MALE", LocalDate.of(1990, 1, 1), 800 - 100 * i),
					null);
		}
		mvc.perform(post("/api/events/{id}/draw", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"poolCount\": 2, \"qualifiersPerPool\": 2}")).andExpect(status().isCreated());
		mvc.perform(post("/api/events/{id}/draw/publish", event).with(organizer(jwtService)))
			.andExpect(status().isOk());
	}

	@Test
	void createsTheMatchesOfEachPoolAndAnEmptyBracket() throws Exception {
		List<Map<String, Object>> matches = matches();

		assertThat(matches).filteredOn((match) -> "A".equals(match.get("pool"))).hasSize(3);
		assertThat(matches).filteredOn((match) -> "B".equals(match.get("pool"))).hasSize(3);
		assertThat(matches).filteredOn((match) -> match.get("bracketPosition") != null)
			.hasSize(3)
			.allMatch((match) -> match.get("sideA") == null && match.get("sideB") == null);
		mvc.perform(get("/api/events/{id}/draw/standings", event))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].pool").value("A"))
			.andExpect(jsonPath("$[0].complete").value(false))
			.andExpect(jsonPath("$[0].rows[0].played").value(0));
	}

	@Test
	void placesTheQualifiersOnceTheirPoolIsOver() throws Exception {
		// The first seed of pool A wins its two matches
		String draw = mvc.perform(get("/api/events/{id}/draw", event).with(organizer(jwtService)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String seed = JsonPath.read(draw, "$.pools[0].entries[0].registrationId");
		for (Map<String, Object> match : matches()) {
			if ("A".equals(match.get("pool"))) {
				walkover(match, seed.equals(match.get("sideB")) ? "B" : "A");
			}
		}

		mvc.perform(get("/api/events/{id}/draw/standings", event))
			.andExpect(jsonPath("$[0].complete").value(true))
			.andExpect(jsonPath("$[0].rows[0].won").value(2))
			.andExpect(jsonPath("$[0].rows[0].gamesWon").value(4))
			.andExpect(jsonPath("$[0].rows[0].pointsWon").value(84))
			.andExpect(jsonPath("$[1].complete").value(false));
		String standings = mvc.perform(get("/api/events/{id}/draw/standings", event))
			.andReturn()
			.getResponse()
			.getContentAsString();
		Set<String> qualifiersOfA = Set.copyOf(JsonPath.read(standings, "$[0].rows[?(@.rank <= 2)].registrationId"));

		Set<Object> inBracket = new HashSet<>();
		for (Map<String, Object> match : matches()) {
			if (match.get("bracketPosition") != null) {
				inBracket.add(match.get("sideA"));
				inBracket.add(match.get("sideB"));
			}
		}
		inBracket.remove(null);
		assertThat(inBracket).isEqualTo(qualifiersOfA);
	}

	private List<Map<String, Object>> matches() throws Exception {
		return JsonPath.read(mvc.perform(get("/api/events/{id}/matches", event))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$");
	}

	private void walkover(Map<String, Object> match, String winner) throws Exception {
		mvc.perform(post("/api/matches/{id}/status", match.get("id")).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"WALKOVER\", \"winner\": \"" + winner + "\", \"stateVersion\": "
					+ match.get("stateVersion") + "}"))
			.andExpect(status().isOk());
	}

}
