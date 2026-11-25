package com.tao.fenuasmash.club;

import java.util.UUID;

/** The `Club` schema. A logo cannot be added yet (`uploadClubLogo` is planned): `logoUrl` is null. */
record ClubResponse(UUID id, String name, String city, String address, String email, String phone, String logoUrl) {

	static ClubResponse from(Club club) {
		return new ClubResponse(club.getId(), club.getName(), club.getCity(), club.getAddress(), club.getEmail(),
				club.getPhone(), null);
	}

}
