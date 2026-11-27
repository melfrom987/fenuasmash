package com.tao.fenuasmash.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.tao.fenuasmash.match.MatchPhase;
import com.tao.fenuasmash.match.MatchSource;
import com.tao.fenuasmash.match.MatchSourceKind;
import com.tao.fenuasmash.match.MatchStatus;
import com.tao.fenuasmash.match.PlannedSlot;

/**
 * Computes and checks the schedule of a tournament, following "Schedule" in
 * `docs/competition-rules.md`: the same rules as the schedule of the web app
 * (`apps/web/src/features/schedule/planner.ts`).
 * <p>
 * Time is cut into slots of the average match length. In each slot, every active court takes
 * the first match that can be played, in this order: event start (with its shift), event
 * order, pools before knockout, round, number. A match can be played once its event has
 * started, its players have rested since their previous match, and the matches it depends on
 * (its pool for a pool rank, the source match for a winner or loser) end the rest time before.
 */
final class SchedulePlanner {

	private static final Set<MatchStatus> PLANNABLE = Set.of(MatchStatus.SCHEDULED, MatchStatus.READY);

	private static final Set<MatchStatus> ONGOING = Set.of(MatchStatus.CALLED, MatchStatus.WARMUP,
			MatchStatus.IN_PROGRESS);

	private static final Duration HORIZON = Duration.ofDays(7);

	private static final long NEVER = Long.MAX_VALUE;

	private SchedulePlanner() {
	}

	/** A match, as the schedule sees it. Sources are only set while their side is unknown. */
	record PlanMatch(UUID id, UUID eventId, MatchStatus status, MatchPhase phase, Integer round, Integer number,
			String pool, UUID sideA, UUID sideB, MatchSource sourceA, MatchSource sourceB, UUID courtId,
			Instant plannedStartAt) {
	}

	/** An event and its planned slot, both ends optional. */
	record PlanEvent(UUID id, Instant plannedDate, Instant plannedEndDate) {
	}

	record PlanCourt(UUID id, int number, boolean active) {
	}

	record StageStart(ScheduleStage stage, Instant at) {
	}

	/** Order, shift and start of an event, saved by the organizer. */
	record EventSetting(UUID eventId, int offsetSlots, Instant startAt, List<StageStart> stageStarts) {
	}

	/**
	 * @param tournamentStart start of the tournament: the start of an event without slot
	 * @param averageMatchMinutes length of a slot
	 * @param minimumRestMinutes rest of a player between two matches
	 * @param players the players of each registration (one, or two for a pair)
	 * @param settings the events in priority order, with their shifts
	 */
	record Input(Instant tournamentStart, int averageMatchMinutes, int minimumRestMinutes, List<PlanEvent> events,
			List<PlanCourt> courts, Map<UUID, List<UUID>> players, List<PlanMatch> matches,
			List<EventSetting> settings) {
	}

	/** The place of every match to play that fits in the horizon of a week. */
	static Map<UUID, PlannedSlot> plan(Input input) {
		long duration = Duration.ofMinutes(input.averageMatchMinutes()).toMillis();
		long rest = Duration.ofMinutes(input.minimumRestMinutes()).toMillis();
		long tournamentStart = input.tournamentStart().toEpochMilli();
		Map<UUID, PlanEvent> events = byId(input.events());
		List<PlanCourt> courts = input.courts()
			.stream()
			.filter(PlanCourt::active)
			.sorted(Comparator.comparingInt(PlanCourt::number))
			.toList();
		Map<UUID, EventSetting> settings = new HashMap<>();
		Map<UUID, Integer> priority = new HashMap<>();
		for (int i = 0; i < input.settings().size(); i++) {
			settings.put(input.settings().get(i).eventId(), input.settings().get(i));
			priority.put(input.settings().get(i).eventId(), i);
		}
		Map<UUID, Integer> lastRound = lastKnockoutRounds(input.matches());

		Map<UUID, Long> slotStart = new HashMap<>();
		Map<UUID, Long> earliest = new HashMap<>();
		for (PlanMatch match : input.matches()) {
			EventSetting setting = settings.get(match.eventId());
			long start = eventStart(events.get(match.eventId()), setting, tournamentStart, duration);
			slotStart.put(match.id(), start);
			long time = start;
			if (setting != null) {
				for (StageStart rule : setting.stageStarts()) {
					if (inStage(match, rule.stage(), lastRound.getOrDefault(match.eventId(), 0))) {
						time = Math.max(time, rule.at().toEpochMilli());
					}
				}
			}
			earliest.put(match.id(), time);
		}
		List<PlanMatch> remaining = new ArrayList<>(input.matches()
			.stream()
			.filter((match) -> PLANNABLE.contains(match.status()))
			.sorted(Comparator.<PlanMatch>comparingLong((match) -> slotStart.get(match.id()))
				.thenComparingInt((match) -> priority.getOrDefault(match.eventId(), Integer.MAX_VALUE))
				.thenComparingInt((match) -> phaseOrder(match.phase()))
				.thenComparingInt((match) -> (match.round() != null) ? match.round() : 0)
				.thenComparingInt((match) -> (match.number() != null) ? match.number() : 0))
			.toList());
		Map<UUID, PlannedSlot> placements = new HashMap<>();
		if (remaining.isEmpty() || courts.isEmpty()) {
			return placements;
		}

		Map<UUID, Long> plannedEnd = new HashMap<>();
		Map<String, Long> playerFree = new HashMap<>();
		Map<UUID, List<UUID>> dependencies = new HashMap<>();
		Set<UUID> plannable = new HashSet<>();
		for (PlanMatch match : remaining) {
			dependencies.put(match.id(), dependenciesOf(match, input.matches()));
			plannable.add(match.id());
		}

		long time = remaining.stream().mapToLong((match) -> slotStart.get(match.id())).min().orElseThrow();
		long limit = time + HORIZON.toMillis();
		long firstSlot = time;
		Set<UUID> occupied = new HashSet<>();
		for (PlanMatch match : input.matches()) {
			if (ONGOING.contains(match.status())) {
				plannedEnd.put(match.id(), firstSlot);
				for (String player : playersOf(match, input.players())) {
					playerFree.put(player, firstSlot);
				}
				if (match.courtId() != null) {
					occupied.add(match.courtId());
				}
			}
		}
		List<Long> starts = new ArrayList<>();

		while (!remaining.isEmpty() && time <= limit) {
			Set<String> busy = new HashSet<>();
			int placedBefore = placements.size();
			for (PlanCourt court : courts) {
				if (time == firstSlot && occupied.contains(court.id())) {
					continue;
				}
				PlanMatch chosen = null;
				for (PlanMatch match : remaining) {
					if (readyAt(match, earliest, dependencies, plannable, plannedEnd, playerFree, rest,
							input.players()) > time) {
						continue;
					}
					if (playersOf(match, input.players()).stream().noneMatch(busy::contains)) {
						chosen = match;
						break;
					}
				}
				if (chosen == null) {
					continue;
				}
				remaining.remove(chosen);
				for (String player : playersOf(chosen, input.players())) {
					busy.add(player);
					playerFree.put(player, time + duration);
				}
				plannedEnd.put(chosen.id(), time + duration);
				placements.put(chosen.id(), new PlannedSlot(Instant.ofEpochMilli(time), court.id()));
				starts.add(time);
			}
			long now = time;
			boolean running = starts.stream().anyMatch((start) -> start + duration > now);
			long next = NEVER;
			for (PlanMatch match : remaining) {
				next = Math.min(next, readyAt(match, earliest, dependencies, plannable, plannedEnd, playerFree, rest,
						input.players()));
			}
			boolean idle = placements.size() == placedBefore && !running;
			time = (idle && next != NEVER && next > time) ? next : time + duration;
		}
		return placements;
	}

	/** The warnings of a schedule, from the planned places of the matches. */
	static List<ScheduleWarning> check(Input input) {
		long duration = Duration.ofMinutes(input.averageMatchMinutes()).toMillis();
		long rest = Duration.ofMinutes(input.minimumRestMinutes()).toMillis();
		Map<UUID, PlanMatch> byId = byIdOf(input.matches());
		List<PlanMatch> planned = input.matches().stream().filter((match) -> match.plannedStartAt() != null).toList();
		List<ScheduleWarning> warnings = new ArrayList<>();

		for (PlanEvent event : input.events()) {
			List<PlanMatch> eventMatches = planned.stream().filter((match) -> match.eventId().equals(event.id())).toList();
			if (eventMatches.isEmpty()) {
				continue;
			}
			if (event.plannedDate() == null) {
				warnings.add(ScheduleWarning.ofEvent("NO_SLOT", event.id(), null));
				continue;
			}
			long start = event.plannedDate().toEpochMilli();
			Long end = (event.plannedEndDate() != null) ? event.plannedEndDate().toEpochMilli() : null;
			long outside = eventMatches.stream()
				.filter((match) -> startOf(match) < start || (end != null && startOf(match) + duration > end))
				.count();
			if (outside > 0) {
				warnings.add(ScheduleWarning.ofEvent("OUTSIDE_SLOT", event.id(), (int) outside));
			}
		}

		Map<String, List<PlanMatch>> byPlayer = new HashMap<>();
		for (PlanMatch match : planned) {
			for (String player : playersOf(match, input.players())) {
				byPlayer.computeIfAbsent(player, (key) -> new ArrayList<>()).add(match);
			}
		}
		Set<UUID> tooShort = new LinkedHashSet<>();
		for (List<PlanMatch> list : byPlayer.values()) {
			list.sort(Comparator.comparingLong(SchedulePlanner::startOf));
			for (int i = 1; i < list.size(); i++) {
				if (startOf(list.get(i)) - (startOf(list.get(i - 1)) + duration) < rest) {
					tooShort.add(list.get(i).id());
				}
			}
		}
		planned.stream()
			.map(PlanMatch::id)
			.filter(tooShort::contains)
			.forEach((id) -> warnings.add(ScheduleWarning.ofMatch("REST_TOO_SHORT", id)));

		for (PlanMatch match : planned) {
			boolean late = dependenciesOf(match, input.matches()).stream().map(byId::get).anyMatch((dependency) ->
					dependency != null && dependency.plannedStartAt() != null && PLANNABLE.contains(dependency.status())
							&& startOf(dependency) + duration > startOf(match));
			if (late) {
				warnings.add(ScheduleWarning.ofMatch("ORDER", match.id()));
			}
		}

		long unscheduled = input.matches()
			.stream()
			.filter((match) -> PLANNABLE.contains(match.status()) && match.plannedStartAt() == null)
			.count();
		if (unscheduled > 0) {
			warnings.add(ScheduleWarning.ofEvent("UNSCHEDULED", null, (int) unscheduled));
		}
		return warnings;
	}

	/** Start of the event: its saved start or its slot, else the tournament start, then its shift. */
	static long eventStart(PlanEvent event, EventSetting setting, long tournamentStart, long duration) {
		long base = (event != null && event.plannedDate() != null) ? event.plannedDate().toEpochMilli()
				: tournamentStart;
		long start = (setting != null && setting.startAt() != null) ? setting.startAt().toEpochMilli() : base;
		return start + ((setting != null) ? setting.offsetSlots() : 0) * duration;
	}

	static boolean inStage(PlanMatch match, ScheduleStage stage, int lastRound) {
		if (match.phase() == MatchPhase.THIRD_PLACE) {
			return true;
		}
		if (match.phase() != MatchPhase.KNOCKOUT || match.round() == null) {
			return false;
		}
		return lastRound - match.round() <= stage.depth();
	}

	/**
	 * Who plays the match: the players of a known side, or a placeholder for the pool rank or
	 * the match result it waits for, so that the same unknown entry is not planned twice at once.
	 */
	static List<String> playersOf(PlanMatch match, Map<UUID, List<UUID>> players) {
		List<String> result = new ArrayList<>();
		addPlayers(result, match, match.sideA(), match.sourceA(), players);
		addPlayers(result, match, match.sideB(), match.sourceB(), players);
		return result;
	}

	private static void addPlayers(List<String> result, PlanMatch match, UUID side, MatchSource source,
			Map<UUID, List<UUID>> players) {
		if (side != null) {
			players.getOrDefault(side, List.of()).forEach((player) -> result.add(player.toString()));
		}
		else if (source != null) {
			result.add((source.kind() == MatchSourceKind.POOL_RANK)
					? match.eventId() + ":" + source.pool() + ":" + source.rank()
					: source.kind() + ":" + source.matchId());
		}
	}

	/** The matches whose end the match waits for: every match of a pool, or a source match. */
	static List<UUID> dependenciesOf(PlanMatch match, List<PlanMatch> matches) {
		Set<UUID> ids = new LinkedHashSet<>();
		for (MatchSource source : new MatchSource[] { match.sourceA(), match.sourceB() }) {
			if (source == null) {
				continue;
			}
			if (source.kind() == MatchSourceKind.POOL_RANK) {
				for (PlanMatch other : matches) {
					if (other.eventId().equals(match.eventId()) && other.phase() == MatchPhase.POOL
							&& source.pool().equals(other.pool())) {
						ids.add(other.id());
					}
				}
			}
			else {
				ids.add(source.matchId());
			}
		}
		return List.copyOf(ids);
	}

	private static long readyAt(PlanMatch match, Map<UUID, Long> earliest, Map<UUID, List<UUID>> dependencies,
			Set<UUID> plannable, Map<UUID, Long> plannedEnd, Map<String, Long> playerFree, long rest,
			Map<UUID, List<UUID>> players) {
		long time = earliest.get(match.id());
		for (UUID id : dependencies.get(match.id())) {
			if (!plannable.contains(id) && !plannedEnd.containsKey(id)) {
				// Over already, or out of the schedule: nothing to wait for
				continue;
			}
			Long end = plannedEnd.get(id);
			if (end == null) {
				return NEVER;
			}
			time = Math.max(time, end + rest);
		}
		for (String player : playersOf(match, players)) {
			Long free = playerFree.get(player);
			if (free != null) {
				time = Math.max(time, free + rest);
			}
		}
		return time;
	}

	private static Map<UUID, Integer> lastKnockoutRounds(List<PlanMatch> matches) {
		Map<UUID, Integer> last = new HashMap<>();
		for (PlanMatch match : matches) {
			if (match.phase() == MatchPhase.KNOCKOUT && match.round() != null) {
				last.merge(match.eventId(), match.round(), Math::max);
			}
		}
		return last;
	}

	private static int phaseOrder(MatchPhase phase) {
		return (phase == null || phase == MatchPhase.POOL) ? 0 : 1;
	}

	private static long startOf(PlanMatch match) {
		return match.plannedStartAt().toEpochMilli();
	}

	private static Map<UUID, PlanEvent> byId(List<PlanEvent> events) {
		Map<UUID, PlanEvent> map = new HashMap<>();
		events.forEach((event) -> map.put(event.id(), event));
		return map;
	}

	private static Map<UUID, PlanMatch> byIdOf(List<PlanMatch> matches) {
		Map<UUID, PlanMatch> map = new HashMap<>();
		matches.forEach((match) -> map.put(match.id(), match));
		return map;
	}

}
