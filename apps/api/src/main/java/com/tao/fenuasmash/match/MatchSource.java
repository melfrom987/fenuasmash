package com.tao.fenuasmash.match;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Where a side of a match comes from while it is not known (`MatchSource` in the contract):
 * a pool rank ({@code pool} and {@code rank}), or the winner or loser of a match
 * ({@code matchId}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchSource(MatchSourceKind kind, String pool, Integer rank, UUID matchId) {

	public static MatchSource poolRank(String pool, int rank) {
		return new MatchSource(MatchSourceKind.POOL_RANK, pool, rank, null);
	}

	public static MatchSource winnerOf(UUID matchId) {
		return new MatchSource(MatchSourceKind.WINNER, null, null, matchId);
	}

}
