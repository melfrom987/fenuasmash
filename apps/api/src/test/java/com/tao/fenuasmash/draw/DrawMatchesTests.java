package com.tao.fenuasmash.draw;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.organizer;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.registration.RegistrationTestSupport;

/** Publishing the draw of a single elimination creates its matches (#92). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DrawMatchesTests extends RegistrationTestSupport {

	@Test
	void createsEveryMatchOfTheBracketWithTheByesInTheSecondRound() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, teva, null);
		registered(event, hina, null);
		registered(event, moana, null);
		mvc.perform(post("/api/events/{id}/draw", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isCreated());

		mvc.perform(get("/api/events/{id}/matches", event)).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(post("/api/events/{id}/draw/publish", event).with(organizer(jwtService)))
			.andExpect(status().isOk());

		// Bracket of 4: Teva (seed 1) faces the bye and waits in the final
		mvc.perform(get("/api/events/{id}/matches", event))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].round").value(1))
			.andExpect(jsonPath("$[0].bracketPosition").value(2))
			.andExpect(jsonPath("$[0].sideA").isNotEmpty())
			.andExpect(jsonPath("$[0].sideB").isNotEmpty())
			.andExpect(jsonPath("$[0].status").value("SCHEDULED"))
			.andExpect(jsonPath("$[1].round").value(2))
			.andExpect(jsonPath("$[1].bracketPosition").value(1))
			.andExpect(jsonPath("$[1].sideB").value(nullValue()));
	}

	@Test
	void createsNoMatchForAHandMadeMatch() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");

		mvc.perform(post("/api/events/{id}/matches", event).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"round\": 1}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.bracketPosition").value(nullValue()));
	}

}
