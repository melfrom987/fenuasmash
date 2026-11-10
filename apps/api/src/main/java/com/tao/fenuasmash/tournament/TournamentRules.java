package com.tao.fenuasmash.tournament;

import java.util.List;

record TournamentRules(
		CompetitionType competitionType,
		DrawFormat drawFormat,
		PlayMode playMode,
		ScoringRules scoring,
		Integer relayStep,
		List<Discipline> disciplines) {
}
