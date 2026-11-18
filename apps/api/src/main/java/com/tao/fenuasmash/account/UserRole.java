package com.tao.fenuasmash.account;

/**
 * What an account is allowed to do. Each account has exactly one role.
 */
public enum UserRole {

	/** Creates the other accounts. */
	ADMIN,

	/** Federation staff: manages the tournaments of its organization. */
	ORGANIZER,

	/** Club manager: manages the players of its club. */
	CLUB,

	/** Player: reads its own record and updates its contact details. */
	PLAYER

}
