package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Which players to list as eligible for an event.
 *
 * @param genders the genders allowed
 * @param bornFrom the earliest birth date allowed, or null
 * @param bornUntil the latest birth date allowed, or null
 * @param excluded players to leave out, e.g. those already registered
 */
public record EligiblePlayers(Set<Gender> genders, LocalDate bornFrom, LocalDate bornUntil,
		Collection<UUID> excluded) {
}
