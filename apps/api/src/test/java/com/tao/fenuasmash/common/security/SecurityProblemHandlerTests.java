package com.tao.fenuasmash.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link SecurityProblemHandler}. Every route is still public,
 * so the handler is called directly; the access rules will exercise it through
 * real requests.
 */
class SecurityProblemHandlerTests {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private final SecurityProblemHandler handler = new SecurityProblemHandler(jsonMapper);

	@Test
	void missingAuthenticationReturnsUnauthorizedProblem() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.commence(request(), response, new InsufficientAuthenticationException("no token"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
		assertThat(body(response)).containsExactlyInAnyOrderEntriesOf(Map.of("type", "about:blank", "title",
				"Unauthorized", "status", 401, "detail", "A valid access token is required", "instance",
				"/api/tournaments", "code", "UNAUTHORIZED"));
	}

	@Test
	void deniedAccessReturnsForbiddenProblem() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(request(), response, new AccessDeniedException("other organization"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getHeader("WWW-Authenticate")).isNull();
		assertThat(body(response)).containsExactlyInAnyOrderEntriesOf(Map.of("type", "about:blank", "title",
				"Forbidden", "status", 403, "detail", "You are not allowed to perform this action", "instance",
				"/api/tournaments", "code", "FORBIDDEN"));
	}

	@Test
	void exceptionMessageIsNotSentToTheClient() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.commence(request(), response, new InsufficientAuthenticationException("secret internal reason"));

		assertThat(response.getContentAsString()).doesNotContain("secret internal reason");
	}

	private static MockHttpServletRequest request() {
		return new MockHttpServletRequest("POST", "/api/tournaments");
	}

	private Map<String, Object> body(MockHttpServletResponse response) throws Exception {
		return jsonMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
		});
	}

}
