package com.tao.fenuasmash.event;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event")
public class Event {

	@Id
	private UUID id;

	@Column(name = "tournament_id", nullable = false, updatable = false)
	private UUID tournamentId;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private EventType type;

	protected Event() {
	}

	Event(UUID tournamentId, String name, EventType type) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		this.name = name;
		this.type = type;
	}

	void update(String name, EventType type) {
		this.name = name;
		this.type = type;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTournamentId() {
		return tournamentId;
	}

	public String getName() {
		return name;
	}

	public EventType getType() {
		return type;
	}

}
