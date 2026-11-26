package com.tao.fenuasmash.club;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
class ClubControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
	}

	@Test
	void createReturnsTheClub() throws Exception {
		create("""
				{"name": " Faa'a Badminton Club ", "city": "Faa'a", "address": "Gymnase de Puurai",
				 "email": "contact@fbc.pf", "phone": "+689 87 00 00 00"}""")
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/clubs/")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.name").value("Faa'a Badminton Club"))
			.andExpect(jsonPath("$.city").value("Faa'a"))
			.andExpect(jsonPath("$.address").value("Gymnase de Puurai"))
			.andExpect(jsonPath("$.email").value("contact@fbc.pf"))
			.andExpect(jsonPath("$.phone").value("+689 87 00 00 00"))
			.andExpect(jsonPath("$.logoUrl").value(nullValue()));
	}

	@Test
	void createLeavesOptionalFieldsEmpty() throws Exception {
		create("""
				{"name": "AS Pirae", "city": "Pirae"}""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.address").value(nullValue()))
			.andExpect(jsonPath("$.email").value(nullValue()))
			.andExpect(jsonPath("$.phone").value(nullValue()));
	}

	@Test
	void createRefusesANameAlreadyUsedWhateverTheCaseAndAccents() throws Exception {
		create("""
				{"name": "Étoile de Papeete", "city": "Papeete"}""").andExpect(status().isCreated());

		create("""
				{"name": "ETOILE de papeete", "city": "Papeete"}""").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CLUB_NAME_ALREADY_EXISTS"));
	}

	@Test
	void createValidatesTheFields() throws Exception {
		create("""
				{"name": " ", "city": "Papeete", "email": "not an email"}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		create("""
				{"name": "AS Pirae"}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void listIsPublicSortedByNameAndFiltered() throws Exception {
		createClub("Taravao BC");
		createClub("Arue Badminton");
		createClub("Élan de Mahina");

		mvc.perform(get("/api/clubs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].name").value("Arue Badminton"))
			.andExpect(jsonPath("$[1].name").value("Élan de Mahina"))
			.andExpect(jsonPath("$[2].name").value("Taravao BC"));

		mvc.perform(get("/api/clubs").param("q", "ELAN"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].name").value("Élan de Mahina"));
	}

	@Test
	void getIsPublic() throws Exception {
		UUID id = createClub("AS Pirae");

		mvc.perform(get("/api/clubs/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("AS Pirae"));
		mvc.perform(get("/api/clubs/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CLUB_NOT_FOUND"));
	}

	@Test
	void updateChangesOnlyTheFieldsSent() throws Exception {
		UUID id = createClub("AS Pirae");
		update(id, """
				{"email": "as@pirae.pf"}""").andExpect(status().isOk());

		update(id, """
				{"city": "Pirae Nord", "phone": "40 00 00 00"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("AS Pirae"))
			.andExpect(jsonPath("$.city").value("Pirae Nord"))
			.andExpect(jsonPath("$.email").value("as@pirae.pf"))
			.andExpect(jsonPath("$.phone").value("40 00 00 00"));
	}

	@Test
	void updateClearsNullableFieldsSentAsNull() throws Exception {
		UUID id = createClub("AS Pirae");
		update(id, """
				{"email": "as@pirae.pf"}""").andExpect(status().isOk());

		update(id, """
				{"email": null}""").andExpect(status().isOk()).andExpect(jsonPath("$.email").value(nullValue()));
	}

	@Test
	void updateRefusesToClearTheNameOrUseAnotherClubName() throws Exception {
		createClub("Arue Badminton");
		UUID id = createClub("AS Pirae");

		update(id, """
				{"name": null}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		update(id, """
				{"name": "arue badminton"}""").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CLUB_NAME_ALREADY_EXISTS"));
		update(id, """
				{"name": "as pirae"}""").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("as pirae"));
	}

	@Test
	void updateReturnsNotFoundForAnUnknownClub() throws Exception {
		update(UUID.randomUUID(), """
				{"city": "Papeete"}""").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CLUB_NOT_FOUND"));
	}

	private UUID createClub(String name) throws Exception {
		String body = create("{\"name\": \"" + name + "\", \"city\": \"Tahiti\"}").andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post("/api/clubs").with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions update(UUID id, String json) throws Exception {
		return mvc.perform(patch("/api/clubs/{id}", id).with(organizer(jwtService))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

}
