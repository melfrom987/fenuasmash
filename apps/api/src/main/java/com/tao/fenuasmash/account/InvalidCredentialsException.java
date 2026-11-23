package com.tao.fenuasmash.account;

import org.springframework.http.HttpStatus;

import com.tao.fenuasmash.common.error.ApiException;

/**
 * The same error for an unknown email and a wrong password, so that the answer
 * never reveals whether an account exists.
 */
class InvalidCredentialsException extends ApiException {

	InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
	}

}
