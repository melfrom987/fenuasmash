package com.tao.fenuasmash.schedule;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A problem of the schedule, shown to the organizer but not blocking (`ScheduleWarning` in
 * the contract).
 *
 * @param code `NO_SLOT`, `OUTSIDE_SLOT`, `REST_TOO_SHORT`, `ORDER` or `UNSCHEDULED`
 * @param eventId the event, for `NO_SLOT` and `OUTSIDE_SLOT`
 * @param matchId the match, for `REST_TOO_SHORT` and `ORDER`
 * @param count the number of matches, for `OUTSIDE_SLOT` and `UNSCHEDULED`
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleWarning(String code, UUID eventId, UUID matchId, Integer count) {

	static ScheduleWarning ofEvent(String code, UUID eventId, Integer count) {
		return new ScheduleWarning(code, eventId, null, count);
	}

	static ScheduleWarning ofMatch(String code, UUID matchId) {
		return new ScheduleWarning(code, null, matchId, null);
	}

}
