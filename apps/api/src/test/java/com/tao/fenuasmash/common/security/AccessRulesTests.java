package com.tao.fenuasmash.common.security;

import static com.tao.fenuasmash.TestTokens.admin;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

/**
 * Who may call which route. The ids are random: a request that passes the
 * access rules gets a 400 or 404 from the controller, one that does not gets a
 * 401 or 403 before reaching it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccessRulesTests {

	private static final UUID ID = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanDatabase() {
		DatabaseCleaner.clean(jdbc);
	}

	@Test
	void readsArePublic() throws Exception {
		for (String url : List.of("/api/organizations", "/api/tournaments", "/actuator/health")) {
			mvc.perform(get(url)).andExpect(status().isOk());
		}
		for (String url : List.of("/api/organizations/" + ID, "/api/tournaments/" + ID,
				"/api/tournaments/" + ID + "/events", "/api/tournaments/" + ID + "/courts", "/api/events/" + ID,
				"/api/courts/" + ID)) {
			mvc.perform(get(url)).andExpect(status().isNotFound());
		}
	}

	@Test
	void writesWithoutTokenAreUnauthorized() throws Exception {
		for (MockHttpServletRequestBuilder request : allWrites()) {
			mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		}
	}

	@Test
	void organizerManagesTournamentsEventsAndCourts() throws Exception {
		for (MockHttpServletRequestBuilder request : organizerWrites()) {
			mvc.perform(request.with(organizer(jwtService)))
				.andExpect(result -> assertThat(result.getResponse().getStatus())
					.as(result.getRequest().getMethod() + " " + result.getRequest().getRequestURI())
					.isIn(400, 404));
		}
	}

	@Test
	void otherRolesCannotManageTournamentsEventsAndCourts() throws Exception {
		for (RequestPostProcessor role : List.of(admin(jwtService), club(jwtService))) {
			for (MockHttpServletRequestBuilder request : organizerWrites()) {
				expectForbidden(request.with(role));
			}
		}
	}

	@Test
	void onlyAdminCreatesOrganizations() throws Exception {
		mvc.perform(createOrganization().with(admin(jwtService))).andExpect(status().isCreated());
		expectForbidden(createOrganization().with(organizer(jwtService)));
		expectForbidden(createOrganization().with(club(jwtService)));
	}

	@Test
	void onlyAdminCreatesAccounts() throws Exception {
		expectForbidden(json(post("/api/accounts"), "{}").with(organizer(jwtService)));
		expectForbidden(json(post("/api/accounts"), "{}").with(club(jwtService)));
	}

	@Test
	void unlistedRoutesAreClosed() throws Exception {
		mvc.perform(post("/api/unknown")).andExpect(status().isUnauthorized());
		expectForbidden(post("/api/unknown").with(organizer(jwtService)));
		expectForbidden(delete("/api/organizations/" + ID).with(admin(jwtService)));
		mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
	}

	private void expectForbidden(MockHttpServletRequestBuilder request) throws Exception {
		mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	private static List<MockHttpServletRequestBuilder> allWrites() {
		List<MockHttpServletRequestBuilder> writes = new ArrayList<>(organizerWrites());
		writes.add(createOrganization());
		writes.add(json(post("/api/accounts"), "{}"));
		return writes;
	}

	private static List<MockHttpServletRequestBuilder> organizerWrites() {
		return List.of(json(post("/api/tournaments"), "{}"), json(patch("/api/tournaments/" + ID), "{}"),
				delete("/api/tournaments/" + ID), json(post("/api/tournaments/" + ID + "/events"), "{}"),
				json(patch("/api/events/" + ID), "{}"), delete("/api/events/" + ID),
				post("/api/tournaments/" + ID + "/courts"), json(patch("/api/courts/" + ID), "{}"),
				delete("/api/courts/" + ID));
	}

	private static MockHttpServletRequestBuilder createOrganization() {
		return json(post("/api/organizations"), "{\"name\": \"Fédération Tahitienne de Badminton\"}");
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
