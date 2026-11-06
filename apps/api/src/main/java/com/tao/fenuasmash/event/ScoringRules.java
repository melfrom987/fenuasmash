package com.tao.fenuasmash.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ScoringRules(
		@Column(name = "games_to_win", nullable = false) int gamesToWin,
		@Column(name = "points_to_win", nullable = false) int pointsToWin,
		@Column(name = "point_cap", nullable = false) int pointCap) {

	static final ScoringRules STANDARD = new ScoringRules(2, 21, 30);

}
