package com.tao.fenuasmash.common.error;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String resource, Object id) {
		super(resource + " " + id + " not found");
	}

}
