package com.tao.fenuasmash.draw;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
record PoolEntryRow(@Column(name = "pool_name", nullable = false) String pool,
		@Column(name = "registration_id", nullable = false) UUID registrationId, @Column(name = "seed") Integer seed) {
}
