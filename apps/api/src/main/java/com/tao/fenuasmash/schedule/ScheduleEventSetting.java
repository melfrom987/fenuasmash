package com.tao.fenuasmash.schedule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/** Order, shift and start of an event in the schedule, kept for the next computations. */
@Entity
@Table(name = "schedule_event_setting")
class ScheduleEventSetting {

	@Id
	@Column(name = "event_id")
	private UUID eventId;

	@Column(name = "tournament_id", nullable = false, updatable = false)
	private UUID tournamentId;

	/** Priority of the event: 0 gets the courts first. */
	@Column(nullable = false)
	private int position;

	@Column(name = "offset_slots", nullable = false)
	private int offsetSlots;

	@Column(name = "start_at")
	private Instant startAt;

	@ElementCollection
	@CollectionTable(name = "schedule_stage_start", joinColumns = @JoinColumn(name = "event_id"))
	private List<StageStartRow> stageStarts = new ArrayList<>();

	protected ScheduleEventSetting() {
	}

	ScheduleEventSetting(UUID eventId, UUID tournamentId, int position, int offsetSlots, Instant startAt,
			List<StageStartRow> stageStarts) {
		this.eventId = eventId;
		this.tournamentId = tournamentId;
		this.position = position;
		this.offsetSlots = offsetSlots;
		this.startAt = (startAt != null) ? startAt.truncatedTo(ChronoUnit.MICROS) : null;
		stageStarts.forEach((row) -> this.stageStarts
			.add(new StageStartRow(row.stage(), row.startsAt().truncatedTo(ChronoUnit.MICROS))));
	}

	UUID getEventId() {
		return eventId;
	}

	int getPosition() {
		return position;
	}

	int getOffsetSlots() {
		return offsetSlots;
	}

	Instant getStartAt() {
		return startAt;
	}

	List<StageStartRow> getStageStarts() {
		return List.copyOf(stageStarts);
	}

}
