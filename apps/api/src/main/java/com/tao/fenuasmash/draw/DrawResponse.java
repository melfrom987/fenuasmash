package com.tao.fenuasmash.draw;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tao.fenuasmash.player.PlayerSummary;

/** The `Draw` schema. */
record DrawResponse(UUID eventId, DrawStatus status, int seedCount, List<Pool> pools, Bracket bracket,
		List<UUID> excludedRegistrationIds, long randomSeed, Instant createdAt, Instant publishedAt) {

	record Entry(UUID registrationId, Integer seed, Integer points, PlayerSummary player, PlayerSummary partner) {
	}

	record Pool(String name, int qualifiers, List<Entry> entries) {
	}

	record Qualifier(String pool, int rank) {
	}

	record Slot(int position, Entry entry, Qualifier qualifier, boolean bye) {
	}

	record Bracket(int size, List<Slot> slots) {
	}

}
