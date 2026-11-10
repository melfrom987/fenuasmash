package com.tao.fenuasmash.event;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tao.fenuasmash.tournament.Discipline;

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
	private Discipline discipline;

	@Column(nullable = false, length = 100)
	private String category;

	@Column(name = "pool_count")
	private Integer poolCount;

	@Column(name = "pool_size")
	private Integer poolSize;

	@Column(name = "qualifiers_per_pool")
	private Integer qualifiersPerPool;

	protected Event() {
	}

	Event(UUID tournamentId, String name, Discipline discipline, String category, PoolStructure structure) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		update(name, discipline, category, structure);
	}

	void update(String name, Discipline discipline, String category, PoolStructure structure) {
		this.name = name;
		this.discipline = discipline;
		this.category = category;
		this.poolCount = structure.poolCount();
		this.poolSize = structure.poolSize();
		this.qualifiersPerPool = structure.qualifiersPerPool();
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

	public Discipline getDiscipline() {
		return discipline;
	}

	public String getCategory() {
		return category;
	}

	public Integer getPoolCount() {
		return poolCount;
	}

	public Integer getPoolSize() {
		return poolSize;
	}

	public Integer getQualifiersPerPool() {
		return qualifiersPerPool;
	}

}
