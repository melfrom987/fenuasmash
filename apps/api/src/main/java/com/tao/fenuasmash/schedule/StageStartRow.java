package com.tao.fenuasmash.schedule;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
record StageStartRow(@Enumerated(EnumType.STRING) @Column(name = "stage", nullable = false, length = 10) ScheduleStage stage,
		@Column(name = "starts_at", nullable = false) Instant startsAt) {
}
