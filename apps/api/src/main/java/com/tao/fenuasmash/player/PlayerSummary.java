package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

/** The `PlayerSummary` schema, inside a registration. */
public record PlayerSummary(UUID id, String firstName, String lastName, Gender gender, LocalDate birthDate,
		String clubName, String licenseNumber, PlayerRankings rankings) {
}
