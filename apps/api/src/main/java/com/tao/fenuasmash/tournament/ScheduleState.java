package com.tao.fenuasmash.tournament;

import java.time.Instant;

/**
 * The state of the schedule of a tournament, and its settings.
 *
 * @param status not generated, draft or published
 * @param generatedAt when it was last computed, or null
 * @param publishedAt when it was published, or null while it is not
 */
public record ScheduleState(ScheduleStatus status, Instant generatedAt, Instant publishedAt) {
}
