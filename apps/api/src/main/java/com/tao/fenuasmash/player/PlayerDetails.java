package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

/** Everything a player is made of, once checked. */
record PlayerDetails(String firstName, String lastName, Gender gender, LocalDate birthDate, UUID clubId,
		String licenseNumber, String email, String phone, PlayerRankings rankings) {
}
