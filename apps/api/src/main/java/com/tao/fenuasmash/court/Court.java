package com.tao.fenuasmash.court;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "court")
public class Court {

	@Id
	private UUID id;

	@Column(name = "tournament_id", nullable = false, updatable = false)
	private UUID tournamentId;

	@Column(nullable = false)
	private int number;

	@Column(nullable = false)
	private boolean active;

	protected Court() {
	}

	Court(UUID tournamentId, int number) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		this.number = number;
		this.active = true;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTournamentId() {
		return tournamentId;
	}

	public int getNumber() {
		return number;
	}

	public boolean isActive() {
		return active;
	}

}
