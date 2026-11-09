package com.tao.fenuasmash.court;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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

	@Test
	void addCreatesActiveCourtWithNextNumberAndLocation() throws Exception {
		insertCourt(tournamentId, 1, true);
		insertCourt(tournamentId, 2, true);

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/courts/")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$.number").value(3))
			.andExpect(jsonPath("$.active").value(true));

		assertThat(countCourts(tournamentId)).isEqualTo(3);
	}

	@Test
	void addStartsAtOneWhenTournamentHasNoCourts() throws Exception {
		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.number").value(1));
	}

	@Test
	void addFollowsHighestNumberWhenNumbersHaveGaps() throws Exception {
		insertCourt(tournamentId, 1, true);
		insertCourt(tournamentId, 3, true);

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.number").value(4));
	}

	@Test
	void addIgnoresCourtsOfOtherTournaments() throws Exception {
		insertCourt(insertTournament("Open de Moorea"), 1, true);

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.number").value(1));
	}

	@Test
	void addRejectsMoreThanFiftyCourts() throws Exception {
		for (int number = 1; number <= 50; number++) {
			insertCourt(tournamentId, number, true);
		}

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_LIMIT_REACHED"));

		assertThat(countCourts(tournamentId)).isEqualTo(50);
	}

	@Test
	void addReturnsNotFoundForUnknownTournament() throws Exception {
		mvc.perform(post("/api/tournaments/{id}/courts", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void findByIdReturnsCourt() throws Exception {
		UUID id = insertCourt(tournamentId, 4, false);

		mvc.perform(get("/api/courts/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id.toString()))
			.andExpect(jsonPath("$.number").value(4))
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownCourt() throws Exception {
		mvc.perform(get("/api/courts/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURT_NOT_FOUND"));
	}

	@Test
	void updateDeactivatesCourt() throws Exception {
		UUID id = insertCourt(tournamentId, 2, true);

		patchJson(id, "{\"active\": false}").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id.toString()))
			.andExpect(jsonPath("$.number").value(2))
			.andExpect(jsonPath("$.active").value(false));

		assertThat(isActive(id)).isFalse();
	}

	@Test
	void updateReactivatesCourt() throws Exception {
		UUID id = insertCourt(tournamentId, 1, false);

		patchJson(id, "{\"active\": true}").andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));

		assertThat(isActive(id)).isTrue();
	}

	@Test
	void updateRejectsMissingActive() throws Exception {
		UUID id = insertCourt(tournamentId, 1, true);

		patchJson(id, "{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("active")));

		assertThat(isActive(id)).isTrue();
	}

	@Test
	void updateReturnsNotFoundForUnknownCourt() throws Exception {
		patchJson(UUID.randomUUID(), "{\"active\": false}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURT_NOT_FOUND"));
	}

	@Test
	void deleteRemovesLastCourt() throws Exception {
		insertCourt(tournamentId, 1, true);
		UUID last = insertCourt(tournamentId, 2, true);

		mvc.perform(delete("/api/courts/{id}", last)).andExpect(status().isNoContent());

		mvc.perform(get("/api/courts/{id}", last)).andExpect(status().isNotFound());
		assertThat(countCourts(tournamentId)).isEqualTo(1);
	}

	@Test
	void deleteAllowsNumberToBeReused() throws Exception {
		insertCourt(tournamentId, 1, true);
		UUID last = insertCourt(tournamentId, 2, true);

		mvc.perform(delete("/api/courts/{id}", last)).andExpect(status().isNoContent());

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.number").value(2));
	}

	@Test
	void deleteRejectsCourtThatIsNotLast() throws Exception {
		UUID first = insertCourt(tournamentId, 1, true);
		insertCourt(tournamentId, 2, true);

		mvc.perform(delete("/api/courts/{id}", first))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_NOT_LAST"));

		assertThat(countCourts(tournamentId)).isEqualTo(2);
	}

	@Test
	void deleteOnlyComparesCourtsOfSameTournament() throws Exception {
		UUID last = insertCourt(tournamentId, 1, true);
		UUID other = insertTournament("Open de Moorea");
		insertCourt(other, 1, true);
		insertCourt(other, 2, true);

		mvc.perform(delete("/api/courts/{id}", last)).andExpect(status().isNoContent());

		assertThat(countCourts(other)).isEqualTo(2);
	}

	@Test
	void deleteReturnsNotFoundForUnknownCourt() throws Exception {
		mvc.perform(delete("/api/courts/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURT_NOT_FOUND"));
	}

	private ResultActions patchJson(UUID id, String body) throws Exception {
		return mvc.perform(patch("/api/courts/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private boolean isActive(UUID id) {
		return jdbc.queryForObject("SELECT active FROM court WHERE id = ?", Boolean.class, id);
	}

	private int countCourts(UUID tournament) {
		return jdbc.queryForObject("SELECT count(*) FROM court WHERE tournament_id = ?", Integer.class, tournament);
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
