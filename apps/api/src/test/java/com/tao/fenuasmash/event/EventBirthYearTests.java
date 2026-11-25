package com.tao.fenuasmash.event;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.organizer;

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
import com.tao.fenuasmash.common.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EventBirthYearTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID tournamentId;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		UUID organizationId = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, 'Ligue', ?)", organizationId,
				Timestamp.from(now));
		tournamentId = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
				    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
				    created_at, updated_at)
				VALUES (?, ?, 'Open', ?, ?, 20, 'INDIVIDUAL', 'SINGLE_ELIMINATION', 'SETS', 2, 21, TRUE, 30, ?, ?)
				""", tournamentId, organizationId, Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), Timestamp.from(now), Timestamp.from(now));
		jdbc.update("INSERT INTO tournament_discipline (tournament_id, sort_order, discipline) VALUES (?, 0, 'MS')",
				tournamentId);
	}

	@Test
	void keepsTheBoundsOfAnAgeEvent() throws Exception {
		create("AGE", "\"Minime U15\"", "2013", "null").andExpect(status().isCreated())
			.andExpect(jsonPath("$.minBirthYear").value(2013))
			.andExpect(jsonPath("$.maxBirthYear").value(nullValue()));
	}

	@Test
	void refusesBoundsOutsideAnAgeEventOrInTheWrongOrder() throws Exception {
		create("OPEN", "null", "2013", "null").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		create("AGE", "\"Cadet\"", "2012", "2010").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void changesAndClearsTheBounds() throws Exception {
		UUID id = idOf(create("AGE", "\"Vétéran V2\"", "null", "1987"));

		update(id, "{\"minBirthYear\": 1960}").andExpect(status().isOk())
			.andExpect(jsonPath("$.minBirthYear").value(1960))
			.andExpect(jsonPath("$.maxBirthYear").value(1987));
		update(id, "{\"maxBirthYear\": null}").andExpect(status().isOk())
			.andExpect(jsonPath("$.maxBirthYear").value(nullValue()));
		// No longer an age event: the bounds go away
		update(id, "{\"categoryType\": \"OPEN\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.minBirthYear").value(nullValue()));
	}

	private ResultActions create(String categoryType, String label, String min, String max) throws Exception {
		return mvc.perform(post("/api/tournaments/{id}/events", tournamentId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Simple", "discipline": "MS", "categoryType": "%s", "categoryLabel": %s,
					 "minBirthYear": %s, "maxBirthYear": %s}""".formatted(categoryType, label, min, max)));
	}

	private ResultActions update(UUID id, String json) throws Exception {
		return mvc.perform(patch("/api/events/{id}", id).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private static UUID idOf(ResultActions result) throws Exception {
		return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
	}

}
