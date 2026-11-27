package com.tao.fenuasmash.schedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Bodies of the schedule routes (`contracts/openapi.yaml`). */
final class ScheduleRequests {

	private ScheduleRequests() {
	}

	/** {@code events}: event order (first = priority) and shifts; null keeps the saved ones. */
	record Generate(List<@Valid @NotNull EventSetting> events) {
	}

	record EventSetting(@NotNull UUID eventId, @NotNull @Min(-100) @Max(100) Integer offsetSlots, Instant startAt,
			List<@Valid @NotNull StageStart> stageStarts) {
	}

	record StageStart(@NotNull ScheduleStage stage, @NotNull Instant at) {
	}

	record Move(@NotNull UUID matchId, @NotNull Instant plannedStartAt, @NotNull UUID courtId) {
	}

}
