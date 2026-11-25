package com.tao.fenuasmash.match;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A match of an event, between two registrations. Its status only changes
 * through {@link #moveTo}, which keeps the result consistent with the status.
 * <p>
 * The match is on {@code courtId} while {@link MatchStatus#isOnCourt()}; it
 * keeps the court once over, and leaves it when sent back to the queue.
 * <p>
 * {@code stateVersion} is a JPA version: two changes made at the same time on
 * the same match cannot both be saved.
 */
@Entity
@Table(name = "match")
class Match {

	@Id
	private UUID id;

	@Column(name = "tournament_id", nullable = false, updatable = false)
	private UUID tournamentId;

	@Column(name = "event_id", nullable = false, updatable = false)
	private UUID eventId;

	private Integer round;

	@Column(name = "side_a_id")
	private UUID sideA;

	@Column(name = "side_b_id")
	private UUID sideB;

	@Column(name = "court_id")
	private UUID courtId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MatchStatus status;

	@Enumerated(EnumType.STRING)
	private MatchSide winner;

	@Enumerated(EnumType.STRING)
	private MatchOutcome outcome;

	@Version
	@Column(name = "state_version", nullable = false)
	private int stateVersion;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Match() {
	}

	Match(UUID tournamentId, UUID eventId, Integer round, UUID sideA, UUID sideB, Instant now) {
		this.id = UUID.randomUUID();
		this.tournamentId = tournamentId;
		this.eventId = eventId;
		this.round = round;
		this.sideA = sideA;
		this.sideB = sideB;
		this.status = MatchStatus.SCHEDULED;
		this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
		this.updatedAt = this.createdAt;
	}

	/**
	 * Changes the status, once the service has checked that the transition and
	 * the result are allowed.
	 */
	void moveTo(MatchStatus next, MatchSide winner, MatchOutcome outcome, Instant now) {
		Instant time = now.truncatedTo(ChronoUnit.MICROS);
		if (next == MatchStatus.IN_PROGRESS && startedAt == null) {
			this.startedAt = time;
		}
		this.endedAt = next.isEnded() ? time : null;
		if (next == MatchStatus.SCHEDULED || next == MatchStatus.READY) {
			this.courtId = null;
		}
		this.status = next;
		this.winner = winner;
		this.outcome = outcome;
		this.updatedAt = time;
	}

	/**
	 * Calls the match to a court, once the service has checked that the court is
	 * free and that no side is on another court.
	 */
	void callTo(UUID court, Instant now) {
		this.courtId = court;
		this.status = MatchStatus.CALLED;
		this.updatedAt = now.truncatedTo(ChronoUnit.MICROS);
	}

	boolean hasBothSides() {
		return sideA != null && sideB != null;
	}

	UUID getId() {
		return id;
	}

	UUID getTournamentId() {
		return tournamentId;
	}

	UUID getEventId() {
		return eventId;
	}

	Integer getRound() {
		return round;
	}

	UUID getSideA() {
		return sideA;
	}

	UUID getSideB() {
		return sideB;
	}

	UUID getCourtId() {
		return courtId;
	}

	MatchStatus getStatus() {
		return status;
	}

	MatchSide getWinner() {
		return winner;
	}

	MatchOutcome getOutcome() {
		return outcome;
	}

	int getStateVersion() {
		return stateVersion;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getEndedAt() {
		return endedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

}
