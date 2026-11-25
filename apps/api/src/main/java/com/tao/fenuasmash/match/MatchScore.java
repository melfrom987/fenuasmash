package com.tao.fenuasmash.match;

import java.util.List;

/**
 * @param games each game played or in progress, in order; empty before the first point
 * @param winner the side that won on points; null while nobody has
 */
public record MatchScore(List<GameScore> games, MatchSide winner) {
}
