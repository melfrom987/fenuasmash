package com.tao.fenuasmash.common.error;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated and its role may call the route, but not on this
 * resource, e.g. a tablet acting on the match of another court.
 */
public class ForbiddenException extends ApiException {

	public ForbiddenException(String code, String message) {
		super(HttpStatus.FORBIDDEN, code, message);
	}

}
