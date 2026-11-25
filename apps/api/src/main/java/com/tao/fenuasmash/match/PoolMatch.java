package com.tao.fenuasmash.match;

import java.util.UUID;

/**
 * A match of a pool, before it is created.
 *
 * @param pool the pool name
 * @param round the round of the pool, from 1
 * @param sideA the registration of side A
 * @param sideB the registration of side B
 */
public record PoolMatch(String pool, int round, UUID sideA, UUID sideB) {
}
