package com.tao.fenuasmash.draw;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "draw")
public class Draw {

	@Id
	@Column(name = "event_id")
	private UUID eventId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DrawStatus status;

	@Column(name = "seed_count", nullable = false)
	private int seedCount;

	@Column(name = "random_seed", nullable = false)
	private long randomSeed;

	/** The settings sent by the organizer, null for the defaults: reused when the draw is run again. */
	@Column(name = "requested_pool_count")
	private Integer requestedPoolCount;

	@Column(name = "requested_qualifiers")
	private Integer requestedQualifiers;

	@Column(name = "requested_seed_count")
	private Integer requestedSeedCount;

	@Column(name = "bracket_size")
	private Integer bracketSize;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@ElementCollection
	@CollectionTable(name = "draw_pool", joinColumns = @JoinColumn(name = "event_id"))
	@OrderColumn(name = "sort_order")
	private List<PoolRow> pools = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "draw_pool_entry", joinColumns = @JoinColumn(name = "event_id"))
	@OrderColumn(name = "sort_order")
	private List<PoolEntryRow> poolEntries = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "draw_bracket_slot", joinColumns = @JoinColumn(name = "event_id"))
	@OrderColumn(name = "sort_order")
	private List<BracketRow> bracket = new ArrayList<>();

	protected Draw() {
	}

	Draw(UUID eventId) {
		this.eventId = eventId;
		this.status = DrawStatus.DRAFT;
	}

	/** Replaces the draft with a new draw. */
	void replace(DrawPlan plan, DrawOptions requested, Instant now) {
		this.seedCount = plan.seedCount();
		this.randomSeed = plan.randomSeed();
		this.requestedPoolCount = requested.poolCount();
		this.requestedQualifiers = requested.qualifiersPerPool();
		this.requestedSeedCount = requested.seedCount();
		this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
		this.pools.clear();
		this.poolEntries.clear();
		for (DrawnPool pool : plan.pools()) {
			this.pools.add(new PoolRow(pool.name(), pool.qualifiers()));
			for (DrawnEntry entry : pool.entries()) {
				this.poolEntries.add(new PoolEntryRow(pool.name(), entry.entrant().registrationId(), entry.seed()));
			}
		}
		this.bracket.clear();
		this.bracketSize = (plan.bracket() != null) ? plan.bracket().size() : null;
		if (plan.bracket() != null) {
			for (BracketSlot slot : plan.bracket().slots()) {
				this.bracket.add(new BracketRow((slot.entry() != null) ? slot.entry().entrant().registrationId() : null,
						(slot.entry() != null) ? slot.entry().seed() : null,
						(slot.qualifier() != null) ? slot.qualifier().pool() : null,
						(slot.qualifier() != null) ? slot.qualifier().rank() : null, slot.bye()));
			}
		}
	}

	void swapPoolEntries(int first, int second) {
		PoolEntryRow a = poolEntries.get(first);
		PoolEntryRow b = poolEntries.get(second);
		poolEntries.set(first, new PoolEntryRow(a.pool(), b.registrationId(), b.seed()));
		poolEntries.set(second, new PoolEntryRow(b.pool(), a.registrationId(), a.seed()));
	}

	void swapBracketLines(int first, int second) {
		BracketRow a = bracket.get(first);
		BracketRow b = bracket.get(second);
		bracket.set(first, a.withContentOf(b));
		bracket.set(second, b.withContentOf(a));
	}

	void publish(Instant now) {
		this.status = DrawStatus.PUBLISHED;
		this.publishedAt = now.truncatedTo(ChronoUnit.MICROS);
	}

	public UUID getEventId() {
		return eventId;
	}

	public DrawStatus getStatus() {
		return status;
	}

	int getSeedCount() {
		return seedCount;
	}

	long getRandomSeed() {
		return randomSeed;
	}

	DrawOptions getRequested() {
		return new DrawOptions(requestedPoolCount, requestedQualifiers, requestedSeedCount);
	}

	Integer getBracketSize() {
		return bracketSize;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getPublishedAt() {
		return publishedAt;
	}

	List<PoolRow> getPools() {
		return pools;
	}

	List<PoolEntryRow> getPoolEntries() {
		return poolEntries;
	}

	List<BracketRow> getBracket() {
		return bracket;
	}

}
