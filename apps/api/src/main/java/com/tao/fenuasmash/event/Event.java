package com.tao.fenuasmash.event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "category_type", nullable = false, length = 5)
	private CategoryType categoryType;

	@Column(name = "category_label", length = 50)
	private String categoryLabel;

	@Column(name = "planned_date")
	private Instant plannedDate;

	@Column(name = "max_entries")
	private Integer maxEntries;

	@Column(name = "pool_count")
	private Integer poolCount;

	@Column(name = "pool_size")
	private Integer poolSize;

	@Column(name = "qualifiers_per_pool")
	private Integer qualifiersPerPool;

	protected Event() {
	}

	Event(UUID tournamentId, String name, Discipline discipline, CategoryType categoryType, String categoryLabel,
			Instant plannedDate, Integer maxEntries, PoolStructure structure) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		update(name, discipline, categoryType, categoryLabel, plannedDate, maxEntries, structure);
	}

	void update(String name, Discipline discipline, CategoryType categoryType, String categoryLabel,
			Instant plannedDate, Integer maxEntries, PoolStructure structure) {
		this.name = name;
		this.discipline = discipline;
		this.categoryType = categoryType;
		this.categoryLabel = categoryLabel;
		this.plannedDate = plannedDate != null ? plannedDate.truncatedTo(ChronoUnit.MICROS) : null;
		this.maxEntries = maxEntries;
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

	public CategoryType getCategoryType() {
		return categoryType;
	}

	public String getCategoryLabel() {
		return categoryLabel;
	}

	public Instant getPlannedDate() {
		return plannedDate;
	}

	public Integer getMaxEntries() {
		return maxEntries;
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
