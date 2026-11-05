package com.tao.fenuasmash.common.error;

import java.util.Locale;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

	public NotFoundException(String resource, Object id) {
		super(HttpStatus.NOT_FOUND, resource.toUpperCase(Locale.ROOT) + "_NOT_FOUND",
				resource + " " + id + " not found");
	}

}
