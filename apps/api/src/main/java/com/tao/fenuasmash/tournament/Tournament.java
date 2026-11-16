package com.tao.fenuasmash.tournament;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "tournament")
public class Tournament {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Column(name = "minimum_rest_minutes", nullable = false)
	private int minimumRestMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "competition_type", nullable = false, length = 10)
	private CompetitionType competitionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "draw_format", nullable = false, length = 30)
	private DrawFormat drawFormat;

	@Enumerated(EnumType.STRING)
	@Column(name = "play_mode", nullable = false, length = 5)
	private PlayMode playMode;

	@Embedded
	private ScoringRules scoring;

	@Column(name = "relay_step")
	private Integer relayStep;

	@ElementCollection
	@CollectionTable(name = "tournament_discipline", joinColumns = @JoinColumn(name = "tournament_id"))
	@OrderColumn(name = "sort_order")
	@Enumerated(EnumType.STRING)
	@Column(name = "discipline", nullable = false, length = 2)
	@BatchSize(size = 50)
	private List<Discipline> disciplines = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "tournament_forbidden_combination", joinColumns = @JoinColumn(name = "tournament_id"))
	@OrderColumn(name = "sort_order")
	@BatchSize(size = 50)
	private List<ForbiddenCombination> forbiddenCombinations = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Tournament() {
	}

	Tournament(UUID organizationId, String name, Instant startsAt, Instant endsAt, int minimumRestMinutes,
			TournamentRules rules) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
		this.name = name;
		this.startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
		this.endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
		this.minimumRestMinutes = minimumRestMinutes;
		applyRules(rules);
		this.createdAt = now;
		this.updatedAt = now;
	}

	void update(String name, Instant startsAt, Instant endsAt, int minimumRestMinutes, TournamentRules rules) {
		this.name = name;
		this.startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
		this.endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
		this.minimumRestMinutes = minimumRestMinutes;
		applyRules(rules);
		this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	private void applyRules(TournamentRules rules) {
		this.competitionType = rules.competitionType();
		this.drawFormat = rules.drawFormat();
		this.playMode = rules.playMode();
		this.scoring = rules.scoring();
		this.relayStep = rules.relayStep();
		this.disciplines.clear();
		this.disciplines.addAll(rules.disciplines());
		this.forbiddenCombinations.clear();
		rules.forbiddenCombinations()
			.forEach(pair -> this.forbiddenCombinations.add(new ForbiddenCombination(pair.get(0), pair.get(1))));
	}

	boolean hasStarted(Instant now) {
		return !startsAt.isAfter(now);
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getName() {
		return name;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public int getMinimumRestMinutes() {
		return minimumRestMinutes;
	}

	TournamentRules getRules() {
		List<List<Discipline>> combinations = forbiddenCombinations.stream()
			.map(combination -> List.of(combination.first(), combination.second()))
			.toList();
		return new TournamentRules(competitionType, drawFormat, playMode, scoring, relayStep, List.copyOf(disciplines),
				combinations);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
