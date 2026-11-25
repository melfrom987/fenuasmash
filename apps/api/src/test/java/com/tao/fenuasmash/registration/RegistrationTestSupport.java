package com.tao.fenuasmash.registration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.organizer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.common.security.JwtService;

/**
 * A tournament with all disciplines, one club and four players: Teva (man, 1995), Manu (man,
 * 2012), Hina (woman, 1998) and Moana (woman, 1985).
 */
abstract class RegistrationTestSupport {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JwtService jwtService;

	@Autowired
	protected JdbcTemplate jdbc;

	protected UUID tournamentId;

	protected UUID teva;

	protected UUID manu;

	protected UUID hina;

	protected UUID moana;

	private UUID clubId;

	@BeforeEach
	void setUpTournamentAndPlayers() {
		DatabaseCleaner.clean(jdbc);
		tournamentId = insertTournament();
		clubId = UUID.randomUUID();
		jdbc.update("INSERT INTO club (id, name, name_key, city) VALUES (?, 'AS Pirae', 'as pirae', 'Pirae')", clubId);
		teva = insertPlayer("Teva", "Tau", "MALE", LocalDate.of(1995, 3, 1), 900);
		manu = insertPlayer("Manu", "Ah-Sing", "MALE", LocalDate.of(2012, 5, 10), 300);
		hina = insertPlayer("Hina", "Tau", "FEMALE", LocalDate.of(1998, 7, 1), 700);
		moana = insertPlayer("Moana", "Lee", "FEMALE", LocalDate.of(1985, 1, 20), null);
	}

	protected ResultActions register(UUID eventId, UUID playerId, UUID partnerId) throws Exception {
		String partner = (partnerId == null) ? "null" : "\"" + partnerId + "\"";
		return mvc.perform(post("/api/events/{id}/registrations", eventId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"playerId\": \"" + playerId + "\", \"partnerId\": " + partner + "}"));
	}

	protected UUID registered(UUID eventId, UUID playerId, UUID partnerId) throws Exception {
		return idOf(register(eventId, playerId, partnerId).andExpect(status().isCreated()));
	}

	protected ResultActions update(UUID registrationId, String json) throws Exception {
		return mvc.perform(patch("/api/registrations/{id}", registrationId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	protected ResultActions transfer(UUID eventId, List<UUID> registrationIds) throws Exception {
		String ids = String.join("\", \"", registrationIds.stream().map(UUID::toString).toList());
		return mvc.perform(post("/api/events/{id}/registrations/transfer", eventId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"registrationIds\": [\"" + ids + "\"]}"));
	}

	protected UUID createEvent(String name, String discipline) throws Exception {
		return createEvent(name, discipline, null);
	}

	protected UUID createEvent(String name, String discipline, Integer maxEntries) throws Exception {
		return postEvent("{\"name\": \"%s\", \"discipline\": \"%s\", \"categoryType\": \"OPEN\", \"maxEntries\": %s}"
			.formatted(name, discipline, maxEntries));
	}

	protected UUID createAgeEvent(String name, String discipline, Integer minBirthYear, Integer maxBirthYear)
			throws Exception {
		return postEvent("""
				{"name": "%s", "discipline": "%s", "categoryType": "AGE", "categoryLabel": "Âge",
				 "minBirthYear": %s, "maxBirthYear": %s}""".formatted(name, discipline, minBirthYear, maxBirthYear));
	}

	protected static UUID idOf(ResultActions result) throws Exception {
		return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private UUID postEvent(String json) throws Exception {
		return idOf(mvc.perform(post("/api/tournaments/{id}/events", tournamentId).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)).andExpect(status().isCreated()));
	}

	private UUID insertTournament() {
		UUID organizationId = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, 'Ligue', ?)", organizationId,
				Timestamp.from(now));
		UUID id = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
				    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
				    created_at, updated_at)
				VALUES (?, ?, 'Open de Papeete', ?, ?, 20, 'INDIVIDUAL', 'SINGLE_ELIMINATION', 'SETS', 2, 21, TRUE, 30,
				    ?, ?)
				""", id, organizationId, Timestamp.from(now.plus(30, ChronoUnit.DAYS)),
				Timestamp.from(now.plus(32, ChronoUnit.DAYS)), Timestamp.from(now), Timestamp.from(now));
		List<String> disciplines = List.of("MS", "WS", "MD", "WD", "XD", "OD");
		for (int position = 0; position < disciplines.size(); position++) {
			jdbc.update("INSERT INTO tournament_discipline (tournament_id, sort_order, discipline) VALUES (?, ?, ?)", id,
					position, disciplines.get(position));
		}
		return id;
	}

	private UUID insertPlayer(String firstName, String lastName, String gender, LocalDate birthDate,
			Integer singlesPoints) {
		UUID id = UUID.randomUUID();
		String key = (lastName + " " + firstName + " | " + firstName + " " + lastName).toLowerCase();
		jdbc.update("""
				INSERT INTO player (id, first_name, last_name, gender, birth_date, club_id, singles_points, search_key)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", id, firstName, lastName, gender, birthDate, clubId, singlesPoints,
				key);
		return id;
	}

}
