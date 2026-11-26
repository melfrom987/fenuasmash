package com.tao.fenuasmash.draw;

/**
 * A bracket line waiting for the result of a pool, e.g. the winner of pool A.
 *
 * @param pool the pool name
 * @param rank the final rank in the pool, from 1
 */
record PoolQualifier(String pool, int rank) {
}
