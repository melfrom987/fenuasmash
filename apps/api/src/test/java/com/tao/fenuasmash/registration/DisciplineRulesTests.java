package com.tao.fenuasmash.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.player.Gender;
import com.tao.fenuasmash.tournament.Discipline;

class DisciplineRulesTests {

	@Test
	void onlySinglesHaveNoPartner() {
		assertThat(DisciplineRules.isSingles(Discipline.MS)).isTrue();
		assertThat(DisciplineRules.isSingles(Discipline.WS)).isTrue();
		for (Discipline doubles : new Discipline[] { Discipline.MD, Discipline.WD, Discipline.XD, Discipline.OD }) {
			assertThat(DisciplineRules.isSingles(doubles)).as(doubles.name()).isFalse();
		}
	}

	@Test
	void matchesTheGenderOfThePlayer() {
		assertThat(DisciplineRules.playerGenders(Discipline.MS)).containsExactly(Gender.MALE);
		assertThat(DisciplineRules.playerGenders(Discipline.MD)).containsExactly(Gender.MALE);
		assertThat(DisciplineRules.playerGenders(Discipline.WS)).containsExactly(Gender.FEMALE);
		assertThat(DisciplineRules.playerGenders(Discipline.WD)).containsExactly(Gender.FEMALE);
		assertThat(DisciplineRules.playerGenders(Discipline.XD)).containsExactlyInAnyOrder(Gender.values());
		assertThat(DisciplineRules.playerGenders(Discipline.OD)).containsExactlyInAnyOrder(Gender.values());
	}

	@Test
	void matchesTheGenderOfThePartner() {
		assertThat(DisciplineRules.partnerGenders(Discipline.MD, Gender.MALE)).containsExactly(Gender.MALE);
		assertThat(DisciplineRules.partnerGenders(Discipline.WD, Gender.FEMALE)).containsExactly(Gender.FEMALE);
		assertThat(DisciplineRules.partnerGenders(Discipline.XD, Gender.MALE)).containsExactly(Gender.FEMALE);
		assertThat(DisciplineRules.partnerGenders(Discipline.XD, Gender.FEMALE)).containsExactly(Gender.MALE);
		assertThat(DisciplineRules.partnerGenders(Discipline.OD, Gender.FEMALE))
			.containsExactlyInAnyOrder(Gender.values());
		assertThat(DisciplineRules.partnerGenders(Discipline.MS, Gender.MALE)).isEmpty();
	}

}
