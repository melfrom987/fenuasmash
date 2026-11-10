package com.tao.fenuasmash.common.error;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
		problem.setTitle(ex.getStatus().getReasonPhrase());
		problem.setProperty("code", ex.getCode());
		return ResponseEntity.status(ex.getStatus()).body(problem);
	}

	@ExceptionHandler(FieldValidationException.class)
	ResponseEntity<ProblemDetail> handleFieldValidation(FieldValidationException ex) {
		ResponseEntity<ProblemDetail> response = handleApiException(ex);
		response.getBody()
			.setProperty("errors", List.of(Map.of("field", ex.getField(), "message", ex.getFieldMessage())));
		return response;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The request conflicts with existing data");
		problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
		problem.setProperty("code", "DATA_CONFLICT");
		return problem;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred");
		problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
		problem.setProperty("code", "INTERNAL_ERROR");
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> Map.of("field", error.getField(), "message",
					Objects.requireNonNullElse(error.getDefaultMessage(), "")))
			.toList();
		ProblemDetail problem = ex.getBody();
		problem.setDetail("Request validation failed");
		problem.setProperty("code", "VALIDATION_FAILED");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

}
