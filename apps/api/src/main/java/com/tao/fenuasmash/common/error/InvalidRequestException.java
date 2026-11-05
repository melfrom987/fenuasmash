package com.tao.fenuasmash.common.error;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {

	public InvalidRequestException(String code, String message) {
		super(HttpStatus.BAD_REQUEST, code, message);
	}

}
