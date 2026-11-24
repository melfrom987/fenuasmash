package com.tao.fenuasmash.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param stateVersion the version of the match as read by the client
 * @param winner required for {@code FINISHED} and {@code WALKOVER}
 * @param outcome required for {@code FINISHED}: {@code NORMAL} or {@code RETIRED}
 */
public record ChangeMatchStatusRequest(
		@NotNull MatchStatus status,
		@NotNull @Min(0) Integer stateVersion,
		MatchSide winner,
		MatchOutcome outcome) {
}
