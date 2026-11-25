package com.tao.fenuasmash.registration;

import java.time.Instant;
import java.util.UUID;

import com.tao.fenuasmash.player.PlayerSummary;

/** The `Registration` schema. */
record RegistrationResponse(UUID id, UUID tournamentId, UUID eventId, RegistrationStatus status, PlayerSummary player,
		PlayerSummary partner, Instant registeredAt, Instant createdAt) {
}
