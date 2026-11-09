package com.tao.fenuasmash.court;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CourtControllerTests {

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
	void listReturnsCourtsSortedByNumber() throws Exception {
		UUID second = insertCourt(tournamentId, 2, true);
		UUID first = insertCourt(tournamentId, 1, true);

		mvc.perform(get("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(first.toString()))
			.andExpect(jsonPath("$[0].tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$[0].number").value(1))
			.andExpect(jsonPath("$[0].active").value(true))
			.andExpect(jsonPath("$[1].id").value(second.toString()))
			.andExpect(jsonPath("$[1].number").value(2));
	}

	@Test
	void listIncludesInactiveCourts() throws Exception {
		insertCourt(tournamentId, 1, false);

		mvc.perform(get("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].active").value(false));
	}

	@Test
	void listExcludesCourtsOfOtherTournaments() throws Exception {
		insertCourt(tournamentId, 1, true);
		insertCourt(insertTournament("Open de Moorea"), 1, true);

		mvc.perform(get("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].tournamentId").value(tournamentId.toString()));
	}

	@Test
	void listReturnsEmptyArrayWhenTournamentHasNoCourts() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void listReturnsNotFoundForUnknownTournament() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/courts", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	private UUID insertTournament(String name) {
		UUID id = UUID.randomUUID();
		Instant startsAt = Instant.now().plus(30, ChronoUnit.DAYS);
		Timestamp now = Timestamp.from(Instant.now());
		jdbc.update(
				"""
						INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes, created_at, updated_at)
						VALUES (?, ?, ?, ?, ?, ?, ?, ?)
						""",
				id, organizationId, name, Timestamp.from(startsAt), Timestamp.from(startsAt.plus(2, ChronoUnit.DAYS)), 20,
				now, now);
		return id;
	}

	private UUID insertCourt(UUID tournament, int number, boolean active) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO court (id, tournament_id, number, active) VALUES (?, ?, ?, ?)", id, tournament, number,
				active);
		return id;
	}

}
