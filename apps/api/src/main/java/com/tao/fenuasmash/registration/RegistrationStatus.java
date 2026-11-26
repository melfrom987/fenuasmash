package com.tao.fenuasmash.registration;

public enum RegistrationStatus {

	/** Singles, or a pair confirmed by both players (or entered by the organizer). */
	CONFIRMED,

	/** The partner has not confirmed yet (imports, later player accounts). */
	PENDING_PARTNER,

	/** Doubles without a partner yet. */
	LOOKING_FOR_PARTNER

}
