package com.tao.fenuasmash.match;

import java.util.Map;

import com.tao.fenuasmash.common.error.ConflictException;

/**
 * Events are missing before the batch: the tablet sends again from
 * {@code expectedSequence}, given in the answer.
 */
class SequenceGapException extends ConflictException {

	private final int expectedSequence;

	SequenceGapException(int expectedSequence, int received) {
		super("SEQUENCE_GAP", "Expected sequence " + expectedSequence + ", received " + received);
		this.expectedSequence = expectedSequence;
	}

	@Override
	public Map<String, Object> getProperties() {
		return Map.of("expectedSequence", expectedSequence);
	}

}
