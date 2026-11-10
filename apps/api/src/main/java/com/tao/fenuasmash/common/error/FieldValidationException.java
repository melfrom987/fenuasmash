package com.tao.fenuasmash.common.error;

import org.springframework.http.HttpStatus;

/**
 * A validation error on one request field that annotations cannot express, because it
 * depends on another field or on the stored data.
 */
public class FieldValidationException extends ApiException {

	private final String field;

	private final String fieldMessage;

	public FieldValidationException(String field, String fieldMessage) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
		this.field = field;
		this.fieldMessage = fieldMessage;
	}

	public String getField() {
		return field;
	}

	public String getFieldMessage() {
		return fieldMessage;
	}

}
