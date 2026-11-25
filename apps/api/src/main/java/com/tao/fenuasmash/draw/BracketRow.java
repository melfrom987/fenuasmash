package com.tao.fenuasmash.draw;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A line of the bracket: an entry, a pool qualifier, or a bye. */
@Embeddable
record BracketRow(@Column(name = "registration_id") UUID registrationId, @Column(name = "seed") Integer seed,
		@Column(name = "qualifier_pool") String qualifierPool, @Column(name = "qualifier_rank") Integer qualifierRank,
		@Column(name = "bye", nullable = false) boolean bye) {

	/** The same line with the content of {@code other}. */
	BracketRow withContentOf(BracketRow other) {
		return new BracketRow(other.registrationId, other.seed, other.qualifierPool, other.qualifierRank, other.bye);
	}

}
