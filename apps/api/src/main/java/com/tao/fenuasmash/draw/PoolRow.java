package com.tao.fenuasmash.draw;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
record PoolRow(@Column(name = "name", nullable = false) String name,
		@Column(name = "qualifiers", nullable = false) int qualifiers) {
}
