package com.tao.fenuasmash.common.security;

import static com.tao.fenuasmash.TestTokens.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTests {

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
	void writeWithoutCsrfTokenIsAcceptedAndCreatesNoSession() throws Exception {
		mvc.perform(post("/api/organizations").with(admin(jwtService))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Club\"}"))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Set-Cookie"))
			.andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
	}

	@Test
	void healthIsPublic() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void httpBasicIsDisabled() throws Exception {
		mvc.perform(get("/api/organizations").header("Authorization", "Basic dXNlcjp3cm9uZw=="))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist("WWW-Authenticate"));
	}

}
