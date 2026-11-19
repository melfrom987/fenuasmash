package com.tao.fenuasmash.common.security;

/**
 * Thrown when an access token is malformed, badly signed, expired or missing a
 * claim, or when a tablet token is unknown or revoked. The message and the
 * cause are kept for logs but never sent to the client: it only gets the
 * {@link #getCode() code}.
 */
public class InvalidTokenException extends RuntimeException {

	static final String DEFAULT_CODE = "UNAUTHORIZED";

	private final String code;

	public InvalidTokenException(String message) {
		this(message, DEFAULT_CODE);
	}

	public InvalidTokenException(String message, String code) {
		super(message);
		this.code = code;
	}

	InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
		this.code = DEFAULT_CODE;
	}

	/** Error code returned with the 401, such as {@code TABLET_REVOKED}. */
	public String getCode() {
		return code;
	}

}
