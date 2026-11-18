package com.tao.fenuasmash.account;

import org.springframework.http.HttpStatus;

import com.tao.fenuasmash.common.error.ApiException;

/**
 * The token is valid but its account no longer exists: the client has to log in
 * again, as with any other unusable token.
 */
class UnknownAccountException extends ApiException {

	UnknownAccountException() {
		super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required");
	}

}
