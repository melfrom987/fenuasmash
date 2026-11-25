package com.tao.fenuasmash.match;

import java.util.UUID;

/**
 * Published, in the transaction of the change, when the status or the score of a pool match
 * changes: its pool may now be over, or no longer.
 */
public record PoolMatchChanged(UUID eventId, String pool) {
}
