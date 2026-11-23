package com.tao.fenuasmash.common.error;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/**
 * 429 with a {@code Retry-After} header telling the client when to try again.
 */
public class TooManyRequestsException extends ApiException {

	private final Duration retryAfter;

	public TooManyRequestsException(String code, String message, Duration retryAfter) {
		super(HttpStatus.TOO_MANY_REQUESTS, code, message);
		this.retryAfter = retryAfter;
	}

	public Duration getRetryAfter() {
		return retryAfter;
	}

}
