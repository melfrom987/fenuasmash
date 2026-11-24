package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

import com.tao.fenuasmash.club.ClubSummary;

/** The `Player` schema. */
record PlayerResponse(UUID id, String firstName, String lastName, Gender gender, LocalDate birthDate, ClubSummary club,
		String licenseNumber, String email, String phone, PlayerRankings rankings) {

	static PlayerResponse from(Player player, ClubSummary club) {
		return new PlayerResponse(player.getId(), player.getFirstName(), player.getLastName(), player.getGender(),
				player.getBirthDate(), club, player.getLicenseNumber(), player.getEmail(), player.getPhone(),
				player.rankings());
	}

}
