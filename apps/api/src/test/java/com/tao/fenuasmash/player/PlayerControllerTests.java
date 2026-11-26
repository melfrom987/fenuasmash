package com.tao.fenuasmash.player;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;

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
class PlayerControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID pirae;

	private UUID arue;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		pirae = insertClub("AS Pirae");
		arue = insertClub("Arue Badminton");
	}

	@Test
	void createReturnsThePlayerWithItsClubAndRankings() throws Exception {
		create("""
				{"firstName": " Rémi ", "lastName": "Le Goff", "gender": "MALE", "birthDate": "2001-04-12",
				 "clubId": "%s", "licenseNumber": "PF-0042", "email": "remi@example.pf", "phone": "87 11 22 33",
				 "rankings": {"singles": {"points": 1250, "position": 3},
				              "doubles": {"points": null, "position": null},
				              "mixed": {"points": 800, "position": 12}}}""".formatted(pirae))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/players/")))
			.andExpect(jsonPath("$.firstName").value("Rémi"))
			.andExpect(jsonPath("$.lastName").value("Le Goff"))
			.andExpect(jsonPath("$.gender").value("MALE"))
			.andExpect(jsonPath("$.birthDate").value("2001-04-12"))
			.andExpect(jsonPath("$.club.id").value(pirae.toString()))
			.andExpect(jsonPath("$.club.name").value("AS Pirae"))
			.andExpect(jsonPath("$.club.logoUrl").value(nullValue()))
			.andExpect(jsonPath("$.licenseNumber").value("PF-0042"))
			.andExpect(jsonPath("$.rankings.singles.points").value(1250))
			.andExpect(jsonPath("$.rankings.singles.position").value(3))
			.andExpect(jsonPath("$.rankings.doubles.points").value(nullValue()))
			.andExpect(jsonPath("$.rankings.mixed.position").value(12));
	}

	@Test
	void createWithoutRankingsGivesEmptyRankings() throws Exception {
		createPlayer("Hina", "Teriierooiterai", "FEMALE", pirae, null)
			.andExpect(jsonPath("$.licenseNumber").value(nullValue()))
			.andExpect(jsonPath("$.rankings.singles.points").value(nullValue()))
			.andExpect(jsonPath("$.rankings.doubles.position").value(nullValue()))
			.andExpect(jsonPath("$.rankings.mixed.points").value(nullValue()));
	}

	@Test
	void createRefusesAnUnknownClub() throws Exception {
		createRaw("Hina", "Tau", "FEMALE", UUID.randomUUID(), null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CLUB"));
	}

	@Test
	void createRefusesALicenseNumberAlreadyUsed() throws Exception {
		createPlayer("Hina", "Tau", "FEMALE", pirae, "PF-1");

		createRaw("Teva", "Tau", "MALE", arue, "PF-1").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PLAYER_LICENSE_ALREADY_EXISTS"));
	}

	@Test
	void createValidatesTheFields() throws Exception {
		create("""
				{"firstName": "Hina", "lastName": "Tau", "gender": "FEMALE", "clubId": "%s"}""".formatted(pirae))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		create("""
				{"firstName": "Hina", "lastName": "Tau", "gender": "FEMALE", "birthDate": "2000-01-01",
				 "clubId": "%s", "rankings": {"singles": {"points": -1, "position": 0},
				 "doubles": {"points": null, "position": null}, "mixed": {"points": null, "position": null}}}"""
			.formatted(pirae)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void searchSortsByLastNameThenFirstNameAndPaginates() throws Exception {
		createPlayer("Teva", "Tau", "MALE", pirae, null);
		createPlayer("Hina", "Tau", "FEMALE", pirae, null);
		createPlayer("Manu", "Ah-Sing", "MALE", arue, null);

		mvc.perform(get("/api/players").with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(3))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.items[0].lastName").value("Ah-Sing"))
			.andExpect(jsonPath("$.items[1].firstName").value("Hina"))
			.andExpect(jsonPath("$.items[2].firstName").value("Teva"))
			.andExpect(jsonPath("$.items[0].club.name").value("Arue Badminton"));

		mvc.perform(get("/api/players").param("page", "1").param("size", "2").with(club(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(3))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].firstName").value("Teva"));
	}

	@Test
	void searchMatchesNamesAndLicenseWithoutCaseOrAccents() throws Exception {
		createPlayer("Rémi", "Le Goff", "MALE", pirae, "PF-0042");
		createPlayer("Hina", "Tau", "FEMALE", arue, null);

		expectFound("REMI", "Rémi");
		expectFound("goff rem", "Rémi");
		expectFound("remi tau", null);
		expectFound("remi le", "Rémi");
		expectFound("pf-0042", "Rémi");
		expectFound("%", null);
	}

	@Test
	void searchFiltersByClubAndGender() throws Exception {
		createPlayer("Teva", "Tau", "MALE", pirae, null);
		createPlayer("Hina", "Tau", "FEMALE", pirae, null);
		createPlayer("Moana", "Tau", "FEMALE", arue, null);

		mvc.perform(get("/api/players").param("clubId", pirae.toString())
			.param("gender", "FEMALE")
			.with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].firstName").value("Hina"));
	}

	@Test
	void searchRefusesInvalidPages() throws Exception {
		mvc.perform(get("/api/players").param("size", "101").with(organizer(jwtService)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		mvc.perform(get("/api/players").param("page", "-1").with(organizer(jwtService)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void getReturnsThePlayerOrNotFound() throws Exception {
		UUID id = idOf(createPlayer("Hina", "Tau", "FEMALE", pirae, null));

		mvc.perform(get("/api/players/{id}", id).with(club(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Hina"));
		mvc.perform(get("/api/players/{id}", UUID.randomUUID()).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
	}

	@Test
	void updateChangesOnlyTheFieldsSentAndClearsNullOnes() throws Exception {
		UUID id = idOf(createPlayer("Hina", "Tau", "FEMALE", pirae, "PF-7"));

		update(id, """
				{"clubId": "%s", "licenseNumber": null, "rankings": {"singles": {"points": 10, "position": 99},
				 "doubles": {"points": null, "position": null}, "mixed": {"points": null, "position": null}}}"""
			.formatted(arue)).andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Hina"))
			.andExpect(jsonPath("$.birthDate").value("2000-01-01"))
			.andExpect(jsonPath("$.club.name").value("Arue Badminton"))
			.andExpect(jsonPath("$.licenseNumber").value(nullValue()))
			.andExpect(jsonPath("$.rankings.singles.position").value(99));

		// The new club and the cleared license are searched too
		expectFound("pf-7", null);
	}

	@Test
	void updateRefusesToClearRequiredFields() throws Exception {
		UUID id = idOf(createPlayer("Hina", "Tau", "FEMALE", pirae, null));

		for (String field : new String[] { "firstName", "lastName", "gender", "birthDate", "clubId", "rankings" }) {
			update(id, "{\"" + field + "\": null}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		}
	}

	@Test
	void updateRefusesAnUnknownClubOrALicenseOfAnotherPlayer() throws Exception {
		createPlayer("Teva", "Tau", "MALE", pirae, "PF-1");
		UUID id = idOf(createPlayer("Hina", "Tau", "FEMALE", pirae, "PF-2"));

		update(id, "{\"clubId\": \"" + UUID.randomUUID() + "\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CLUB"));
		update(id, """
				{"licenseNumber": "PF-1"}""").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PLAYER_LICENSE_ALREADY_EXISTS"));
		update(id, """
				{"licenseNumber": "PF-2"}""").andExpect(status().isOk());
	}

	private void expectFound(String query, String firstName) throws Exception {
		ResultActions result = mvc.perform(get("/api/players").param("q", query).with(organizer(jwtService)))
			.andExpect(status().isOk());
		if (firstName == null) {
			result.andExpect(jsonPath("$.totalItems").value(0));
		}
		else {
			result.andExpect(jsonPath("$.totalItems").value(1)).andExpect(jsonPath("$.items[0].firstName").value(firstName));
		}
	}

	private UUID insertClub(String name) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO club (id, name, name_key, city) VALUES (?, ?, ?, ?)", id, name,
				name.toLowerCase(), "Tahiti");
		return id;
	}

	private ResultActions createPlayer(String firstName, String lastName, String gender, UUID clubId,
			String licenseNumber) throws Exception {
		return createRaw(firstName, lastName, gender, clubId, licenseNumber).andExpect(status().isCreated());
	}

	private ResultActions createRaw(String firstName, String lastName, String gender, UUID clubId,
			String licenseNumber) throws Exception {
		String license = (licenseNumber == null) ? "null" : "\"" + licenseNumber + "\"";
		return create("""
				{"firstName": "%s", "lastName": "%s", "gender": "%s", "birthDate": "2000-01-01",
				 "clubId": "%s", "licenseNumber": %s}""".formatted(firstName, lastName, gender, clubId, license));
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post("/api/players").with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions update(UUID id, String json) throws Exception {
		return mvc.perform(patch("/api/players/{id}", id).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private static UUID idOf(ResultActions result) throws Exception {
		return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
	}

}
