package com.tao.fenuasmash.match;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * A point or an undo entered on the umpire tablet. The id comes from the
 * tablet: the same event sent again keeps it, which tells duplicates apart.
 */
@Entity
@Table(name = "score_event")
class ScoreEvent implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column(name = "match_id", nullable = false, updatable = false)
	private UUID matchId;

	@Column(nullable = false, updatable = false)
	private int sequence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ScoreEventType type;

	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private MatchSide side;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@Column(name = "tablet_id", updatable = false)
	private UUID tabletId;

	/** The id is set by the tablet: tells Spring Data to insert, without reading first. */
	@Transient
	private boolean isNew;

	protected ScoreEvent() {
	}

	ScoreEvent(UUID matchId, ScoreEventRequest request, UUID tabletId, Instant now) {
		this.id = request.id();
		this.matchId = matchId;
		this.sequence = request.sequence();
		this.type = request.type();
		this.side = request.side();
		this.occurredAt = request.occurredAt().truncatedTo(ChronoUnit.MICROS);
		this.receivedAt = now.truncatedTo(ChronoUnit.MICROS);
		this.tabletId = tabletId;
		this.isNew = true;
	}

	/** Whether the request is this event sent again, with the same content. */
	boolean isSameAs(UUID otherMatchId, ScoreEventRequest request) {
		return matchId.equals(otherMatchId) && sequence == request.sequence() && type == request.type()
				&& side == request.side()
				&& occurredAt.equals(request.occurredAt().truncatedTo(ChronoUnit.MICROS));
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	int getSequence() {
		return sequence;
	}

	ScoreEventType getType() {
		return type;
	}

	MatchSide getSide() {
		return side;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ScoreEvent event && Objects.equals(id, event.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}
