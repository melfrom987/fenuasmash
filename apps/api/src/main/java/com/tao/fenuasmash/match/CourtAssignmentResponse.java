package com.tao.fenuasmash.match;

import com.tao.fenuasmash.court.CourtResponse;

/**
 * @param match the match on the court; null when the court is free
 */
public record CourtAssignmentResponse(CourtResponse court, MatchResponse match) {
}
