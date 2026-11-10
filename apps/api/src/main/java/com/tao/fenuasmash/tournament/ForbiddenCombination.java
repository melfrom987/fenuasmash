package com.tao.fenuasmash.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
record ForbiddenCombination(
		@Enumerated(EnumType.STRING) @Column(name = "first_discipline", nullable = false, length = 2) Discipline first,
		@Enumerated(EnumType.STRING) @Column(name = "second_discipline", nullable = false, length = 2) Discipline second) {
}
