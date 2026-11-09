package com.tao.fenuasmash.event;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EventFormat format;

	@Column(name = "pool_size")
	private Integer poolSize;

	@Column(name = "qualifiers_per_pool")
	private Integer qualifiersPerPool;

	@Embedded
	private ScoringRules scoring;

	protected Event() {
	}

	Event(UUID tournamentId, String name, EventType type, EventSettings settings) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		update(name, type, settings);
	}

	void update(String name, EventType type, EventSettings settings) {
		this.name = name;
		this.type = type;
		this.format = settings.format();
		this.poolSize = settings.poolSize();
		this.qualifiersPerPool = settings.qualifiersPerPool();
		this.scoring = settings.scoring();
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

	public EventFormat getFormat() {
		return format;
	}

	public Integer getPoolSize() {
		return poolSize;
	}

	public Integer getQualifiersPerPool() {
		return qualifiersPerPool;
	}

	public ScoringRules getScoring() {
		return scoring;
	}

}
