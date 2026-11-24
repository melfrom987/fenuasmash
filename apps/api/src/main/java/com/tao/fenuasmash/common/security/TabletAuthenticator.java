package com.tao.fenuasmash.common.security;

/**
 * Checks a tablet token against the paired tablets. Implemented by the tablet
 * feature, so that security does not depend on it.
 */
public interface TabletAuthenticator {

	/**
	 * Finds the tablet holding this token and records its activity.
	 * @throws InvalidTokenException if no tablet holds the token, or if it was
	 * revoked
	 */
	TabletPrincipal authenticate(String token);

}
