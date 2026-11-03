package com.tao.fenuasmash.organization;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.tao.fenuasmash.TestcontainersConfiguration;

/**
 * Integration tests for {@code /api/organizations}, running against a real
 * PostgreSQL database started by Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationControllerTests {

	private static final String URL = "/api/organizations";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private OrganizationRepository repository;

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAll();
	}

	@Test
	void createReturnsCreatedOrganizationWithLocation() throws Exception {
		create("{\"name\": \"Ligue de Badminton de Polynésie\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.name").value("Ligue de Badminton de Polynésie"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(header().string("Location", startsWith(URL + "/")));
	}

	@Test
	void createTrimsName() throws Exception {
		create("{\"name\": \"  AS Faa'a  \"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("AS Faa'a"));
	}

	@Test
	void createRejectsBlankName() throws Exception {
		assertBadRequest(create("{\"name\": \"   \"}"));
	}

	@Test
	void createRejectsMissingName() throws Exception {
		assertBadRequest(create("{}"));
	}

	@Test
	void createRejectsNameLongerThan200Characters() throws Exception {
		assertBadRequest(create("{\"name\": \"" + "a".repeat(201) + "\"}"));
	}

	@Test
	void createRejectsMalformedJson() throws Exception {
		assertBadRequest(create("{oops"));
	}

	@Test
	void findAllReturnsOrganizationsSortedByName() throws Exception {
		repository.save(new Organization("Ligue de Badminton de Polynésie"));
		repository.save(new Organization("AS Faa'a"));

		mvc.perform(get(URL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].name").value("AS Faa'a"))
			.andExpect(jsonPath("$[1].name").value("Ligue de Badminton de Polynésie"));
	}

	@Test
	void findAllReturnsEmptyListWhenNoOrganization() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void findByIdReturnsOrganization() throws Exception {
		Organization organization = repository.save(new Organization("AS Faa'a"));

		mvc.perform(get(URL + "/" + organization.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(organization.getId().toString()))
			.andExpect(jsonPath("$.name").value("AS Faa'a"));
	}

	@Test
	void findByIdReturnsNotFoundForUnknownId() throws Exception {
		UUID unknownId = UUID.randomUUID();

		mvc.perform(get(URL + "/" + unknownId))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Resource not found"))
			.andExpect(jsonPath("$.detail").value(endsWith(unknownId + " not found")));
	}

	@Test
	void findByIdReturnsBadRequestForMalformedId() throws Exception {
		assertBadRequest(mvc.perform(get(URL + "/not-a-uuid")));
	}

	private ResultActions create(String json) throws Exception {
		return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static void assertBadRequest(ResultActions result) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

}
