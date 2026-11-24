package com.tao.fenuasmash.common.security;

import java.util.UUID;

/**
 * The authenticated principal of a request sent by a paired umpire tablet,
 * with {@code ROLE_TABLET} as authority.
 *
 * @param tabletId the tablet
 * @param courtId the court the tablet is paired with
 */
public record TabletPrincipal(UUID tabletId, UUID courtId) {

	/** Tablet tokens start with this prefix, which tells them apart from JWTs. */
	public static final String TOKEN_PREFIX = "fst_";

	public static final String ROLE = "TABLET";

}
