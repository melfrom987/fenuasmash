package com.tao.fenuasmash.common.security;

/**
 * Thrown when an access token is malformed, badly signed, expired or missing a
 * claim. The cause is kept for logs but never sent to the client.
 */
public class InvalidTokenException extends RuntimeException {

	InvalidTokenException(String message) {
		super(message);
	}

	InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}

}
