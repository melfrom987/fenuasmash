package com.tao.fenuasmash.registration;

import java.util.EnumSet;
import java.util.Set;

import com.tao.fenuasmash.player.Gender;
import com.tao.fenuasmash.tournament.Discipline;

/** Who may play which discipline: see "Registrations" in `docs/competition-rules.md`. */
final class DisciplineRules {

	private DisciplineRules() {
	}

	static boolean isSingles(Discipline discipline) {
		return discipline == Discipline.MS || discipline == Discipline.WS;
	}

	/** Genders allowed for the registered player. */
	static Set<Gender> playerGenders(Discipline discipline) {
		return switch (discipline) {
			case MS, MD -> EnumSet.of(Gender.MALE);
			case WS, WD -> EnumSet.of(Gender.FEMALE);
			case XD, OD -> EnumSet.allOf(Gender.class);
		};
	}

	/** Genders allowed for the partner of a player of {@code playerGender}. */
	static Set<Gender> partnerGenders(Discipline discipline, Gender playerGender) {
		return switch (discipline) {
			case MS, WS -> EnumSet.noneOf(Gender.class);
			case MD -> EnumSet.of(Gender.MALE);
			case WD -> EnumSet.of(Gender.FEMALE);
			case XD -> EnumSet.of((playerGender == Gender.MALE) ? Gender.FEMALE : Gender.MALE);
			case OD -> EnumSet.allOf(Gender.class);
		};
	}

}
