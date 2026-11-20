package com.tao.fenuasmash.common.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes 401 and 403 responses as Problem Details with a {@code code}, like
 * every other API error. These errors are raised by the security filters,
 * before the request reaches a controller, so the global exception handler
 * never sees them.
 */
@Component
class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";

	static final String FORBIDDEN_CODE = "FORBIDDEN";

	private final JsonMapper jsonMapper;

	SecurityProblemHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		// A revoked tablet gets its own code, so that it asks for a new pairing
		String code = authException.getCause() instanceof InvalidTokenException invalidToken ? invalidToken.getCode()
				: UNAUTHORIZED_CODE;
		write(request, response, HttpStatus.UNAUTHORIZED, code, "A valid access token is required");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(request, response, HttpStatus.FORBIDDEN, FORBIDDEN_CODE, "You are not allowed to perform this action");
	}

	private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code,
			String detail) throws IOException {
		Map<String, Object> problem = new LinkedHashMap<>();
		problem.put("type", "about:blank");
		problem.put("title", status.getReasonPhrase());
		problem.put("status", status.value());
		problem.put("detail", detail);
		problem.put("instance", request.getRequestURI());
		problem.put("code", code);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
