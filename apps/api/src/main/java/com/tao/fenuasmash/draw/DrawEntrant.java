package com.tao.fenuasmash.draw;

import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

/**
 * A confirmed registration taking part in the draw: a player or a pair.
 *
 * @param registrationId the registration
 * @param points ranking points in the discipline (sum for a pair), or null if unranked
 * @param registeredAt when the registration was made, to break ties
 * @param clubs the clubs of the player and of the partner, to keep them apart
 */
record DrawEntrant(UUID registrationId, Double points, Instant registeredAt, Set<String> clubs) {

	/**
	 * Ranking order of `docs/competition-rules.md`: most points first (unranked last), then
	 * the earliest registration. The registration id only makes the order total.
	 */
	static final Comparator<DrawEntrant> RANKING = Comparator
		.comparing(DrawEntrant::points, Comparator.nullsLast(Comparator.reverseOrder()))
		.thenComparing(DrawEntrant::registeredAt)
		.thenComparing(DrawEntrant::registrationId);

	DrawEntrant {
		clubs = Set.copyOf(clubs);
	}

	boolean sharesClubWith(DrawEntrant other) {
		return this.clubs.stream().anyMatch(other.clubs::contains);
	}

}
