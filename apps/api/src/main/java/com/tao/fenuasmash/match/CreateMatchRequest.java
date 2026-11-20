package com.tao.fenuasmash.match;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param round round in the draw, null when the match is not part of a draw
 * @param sideA registration of side A, null while unknown
 * @param sideB registration of side B, null while unknown
 */
public record CreateMatchRequest(@Min(1) @Max(20) Integer round, UUID sideA, UUID sideB) {
}
