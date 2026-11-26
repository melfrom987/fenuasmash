package com.tao.fenuasmash.club;

import java.util.UUID;

/** The `ClubSummary` schema, inside a player. */
public record ClubSummary(UUID id, String name, String logoUrl) {

	static ClubSummary from(Club club) {
		return new ClubSummary(club.getId(), club.getName(), null);
	}

}
