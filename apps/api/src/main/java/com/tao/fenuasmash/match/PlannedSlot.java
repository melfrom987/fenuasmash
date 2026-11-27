package com.tao.fenuasmash.match;

import java.time.Instant;
import java.util.UUID;

/**
 * The place of a match in the schedule.
 *
 * @param startAt the planned start
 * @param courtId the planned court
 */
public record PlannedSlot(Instant startAt, UUID courtId) {
}
