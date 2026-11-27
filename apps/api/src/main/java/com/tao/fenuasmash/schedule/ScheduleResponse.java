package com.tao.fenuasmash.schedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tao.fenuasmash.tournament.ScheduleStatus;

/**
 * The schedule of a tournament (`Schedule` in the contract). The planned times and courts are
 * on the matches.
 *
 * @param events the events with matches, in schedule order
 */
record ScheduleResponse(UUID tournamentId, ScheduleStatus status, List<Event> events, Instant generatedAt,
		Instant publishedAt, Instant firstStartAt, Instant lastEndAt, List<ScheduleWarning> warnings) {

	record Event(UUID eventId, int offsetSlots, Instant startAt, List<StageStart> stageStarts, int matchCount,
			Instant firstStartAt, Instant lastEndAt) {
	}

	record StageStart(ScheduleStage stage, Instant at) {
	}

}
