package com.tao.fenuasmash.match;

/**
 * A match waiting for a court.
 *
 * @param available no side of the match is on a court: it can be called now
 */
public record QueuedMatchResponse(MatchResponse match, boolean available) {
}
