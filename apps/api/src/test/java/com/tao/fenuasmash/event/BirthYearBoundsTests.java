package com.tao.fenuasmash.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BirthYearBoundsTests {

	@Test
	void letsYoungerPlayersPlayUpInAYouthEvent() {
		// Minime U15 in 2026-2027: born in 2013 or later
		BirthYearBounds minime = new BirthYearBounds(2013, null);

		assertThat(minime.allows(LocalDate.of(2013, 1, 1))).isTrue();
		assertThat(minime.allows(LocalDate.of(2016, 6, 30))).isTrue();
		assertThat(minime.allows(LocalDate.of(2012, 12, 31))).isFalse();
	}

	@Test
	void onlyLetsOldEnoughPlayersInAVeteranEvent() {
		// Veteran V2 in 2026-2027: born in 1987 or earlier
		BirthYearBounds veteran = new BirthYearBounds(null, 1987);

		assertThat(veteran.allows(LocalDate.of(1987, 12, 31))).isTrue();
		assertThat(veteran.allows(LocalDate.of(1960, 1, 1))).isTrue();
		assertThat(veteran.allows(LocalDate.of(1988, 1, 1))).isFalse();
	}

	@Test
	void letsEveryoneInWithoutBounds() {
		assertThat(BirthYearBounds.NONE.allows(LocalDate.of(1950, 1, 1))).isTrue();
		assertThat(BirthYearBounds.NONE.allows(LocalDate.of(2020, 1, 1))).isTrue();
	}

}
