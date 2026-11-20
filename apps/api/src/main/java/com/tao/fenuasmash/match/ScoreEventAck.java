package com.tao.fenuasmash.match;

import java.util.List;
import java.util.UUID;

/**
 * @param acceptedIds events applied by this request
 * @param duplicateIds events received before, not applied again
 * @param lastSequence highest sequence applied for the match
 */
public record ScoreEventAck(List<UUID> acceptedIds, List<UUID> duplicateIds, int lastSequence, MatchScore score,
		MatchStatus status, int stateVersion) {
}
