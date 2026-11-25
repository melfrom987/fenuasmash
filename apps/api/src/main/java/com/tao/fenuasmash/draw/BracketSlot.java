package com.tao.fenuasmash.draw;

/**
 * A line of the knockout bracket: an entry, a pool qualifier or a bye. Lines 2k-1 and 2k meet
 * in the first round.
 *
 * @param position the line, from 1
 * @param entry the entry on the line, for a single elimination
 * @param qualifier the pool qualifier on the line, for pools then knockout
 * @param bye true when the opponent of the other line goes through without playing
 */
record BracketSlot(int position, DrawnEntry entry, PoolQualifier qualifier, boolean bye) {
}
