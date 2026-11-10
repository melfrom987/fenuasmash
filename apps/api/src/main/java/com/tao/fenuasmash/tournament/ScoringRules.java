package com.tao.fenuasmash.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ScoringRules(
		@Column(name = "games_to_win", nullable = false) int gamesToWin,
		@Column(name = "points_to_win", nullable = false) int pointsToWin,
		@Column(name = "two_point_lead", nullable = false) boolean twoPointLead,
		@Column(name = "point_cap") Integer pointCap) {
}
