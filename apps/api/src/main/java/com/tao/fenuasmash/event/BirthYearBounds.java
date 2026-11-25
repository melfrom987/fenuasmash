package com.tao.fenuasmash.event;

import java.time.LocalDate;

/**
 * Birth years allowed in an age event; a null bound is open. See "Birth-year bounds" in
 * `docs/competition-rules.md`.
 *
 * @param min the oldest birth year allowed (youth: younger players may play up)
 * @param max the most recent birth year allowed (veterans)
 */
public record BirthYearBounds(Integer min, Integer max) {

	public static final BirthYearBounds NONE = new BirthYearBounds(null, null);

	public boolean allows(LocalDate birthDate) {
		int year = birthDate.getYear();
		return (min == null || year >= min) && (max == null || year <= max);
	}

}
