package com.tao.fenuasmash.registration;

import java.util.List;
import java.util.UUID;

/** The `TransferResult` schema. */
record TransferResult(List<RegistrationResponse> moved, List<Rejection> rejected) {

	record Rejection(UUID registrationId, String code, String message) {
	}

}
