package com.tao.fenuasmash.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.match.MatchPhase;
import com.tao.fenuasmash.match.MatchSource;
import com.tao.fenuasmash.match.MatchStatus;
import com.tao.fenuasmash.match.PlannedSlot;
import com.tao.fenuasmash.schedule.SchedulePlanner.EventSetting;
import com.tao.fenuasmash.schedule.SchedulePlanner.Input;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanCourt;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanEvent;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanMatch;
import com.tao.fenuasmash.schedule.SchedulePlanner.StageStart;

class SchedulePlannerTests {

	private static final Instant START = Instant.parse("2026-12-05T08:00:00Z");

	private static final UUID EVENT = new UUID(1, 1);

	private static final PlanCourt COURT_1 = new PlanCourt(new UUID(2, 1), 1, true);

	private static final PlanCourt COURT_2 = new PlanCourt(new UUID(2, 2), 2, true);

	/** Each registration is one player of the same id. */
	private final Map<UUID, List<UUID>> players = new HashMap<>();

	private int next = 1;

	private UUID entry() {
		UUID registration = new UUID(3, next++);
		players.put(registration, List.of(new UUID(4, registration.getLeastSignificantBits())));
		return registration;
	}

	private PlanMatch match(MatchStatus status, MatchPhase phase, Integer round, String pool, UUID a, UUID b,
			MatchSource sourceA, MatchSource sourceB) {
		int number = next++;
		return new PlanMatch(new UUID(5, number), EVENT, status, phase, round, number, pool, a, b, sourceA, sourceB,
				null, null);
	}

	private PlanMatch poolMatch(String pool, int round, UUID a, UUID b) {
		return match(MatchStatus.SCHEDULED, MatchPhase.POOL, round, pool, a, b, null, null);
	}

	private Input input(List<PlanMatch> matches, List<PlanCourt> courts, List<EventSetting> settings,
			PlanEvent... events) {
		List<PlanEvent> list = (events.length > 0) ? List.of(events) : List.of(new PlanEvent(EVENT, null, null));
		return new Input(START, 25, 20, list, courts, players, matches, settings);
	}

	private static Instant at(int minutes) {
		return START.plus(Duration.ofMinutes(minutes));
	}

	private static PlanMatch planned(PlanMatch match, Map<UUID, PlannedSlot> plan) {
		PlannedSlot slot = plan.get(match.id());
		return new PlanMatch(match.id(), match.eventId(), match.status(), match.phase(), match.round(), match.number(),
				match.pool(), match.sideA(), match.sideB(), match.sourceA(), match.sourceB(), match.courtId(),
				(slot != null) ? slot.startAt() : null);
	}

	@Test
	void fillsTheActiveCourtsSlotBySlotInNumberOrder() {
		List<PlanMatch> matches = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			matches.add(poolMatch("A", 1, entry(), entry()));
		}
		PlanCourt inactive = new PlanCourt(new UUID(2, 3), 3, false);

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(matches, List.of(COURT_2, inactive, COURT_1),
				List.of()));

		assertThat(plan.get(matches.get(0).id())).isEqualTo(new PlannedSlot(at(0), COURT_1.id()));
		assertThat(plan.get(matches.get(1).id())).isEqualTo(new PlannedSlot(at(0), COURT_2.id()));
		assertThat(plan.get(matches.get(2).id())).isEqualTo(new PlannedSlot(at(25), COURT_1.id()));
		assertThat(plan.get(matches.get(3).id())).isEqualTo(new PlannedSlot(at(25), COURT_2.id()));
	}

	@Test
	void letsAPlayerRestBetweenTwoMatches() {
		UUID teva = entry();
		PlanMatch first = poolMatch("A", 1, teva, entry());
		PlanMatch other = poolMatch("B", 1, entry(), entry());
		PlanMatch second = poolMatch("A", 2, teva, entry());
		PlanMatch third = poolMatch("B", 2, entry(), entry());

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(first, other, second, third),
				List.of(COURT_1, COURT_2), List.of()));

		// 25 min of match then 20 min of rest: not in the 2nd slot (25), in the 3rd (50)
		assertThat(plan.get(first.id()).startAt()).isEqualTo(at(0));
		assertThat(plan.get(third.id()).startAt()).isEqualTo(at(25));
		assertThat(plan.get(second.id()).startAt()).isEqualTo(at(50));
	}

	@Test
	void plansAWinnerAfterItsSourceMatchAndTheRest() {
		PlanMatch semi = match(MatchStatus.READY, MatchPhase.KNOCKOUT, 1, null, entry(), entry(), null, null);
		PlanMatch otherSemi = match(MatchStatus.READY, MatchPhase.KNOCKOUT, 1, null, entry(), entry(), null, null);
		PlanMatch fin = match(MatchStatus.SCHEDULED, MatchPhase.KNOCKOUT, 2, null, null, null,
				MatchSource.winnerOf(semi.id()), MatchSource.winnerOf(otherSemi.id()));

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(fin, semi, otherSemi),
				List.of(COURT_1, COURT_2), List.of()));

		assertThat(plan.get(semi.id()).startAt()).isEqualTo(at(0));
		// Semi-finals end at 25, then 20 min of rest: the final starts at 45, the court being idle
		assertThat(plan.get(fin.id()).startAt()).isEqualTo(at(45));
	}

	@Test
	void plansAPoolQualifierOnceTheWholePoolIsOver() {
		UUID a = entry();
		UUID b = entry();
		UUID c = entry();
		PlanMatch ab = poolMatch("A", 1, a, b);
		PlanMatch bc = poolMatch("A", 2, b, c);
		PlanMatch ac = poolMatch("A", 3, a, c);
		PlanMatch knockout = match(MatchStatus.SCHEDULED, MatchPhase.KNOCKOUT, 1, null, null, entry(),
				MatchSource.poolRank("A", 1), null);

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(ab, bc, ac, knockout),
				List.of(COURT_1, COURT_2), List.of()));

		Instant poolEnd = plan.get(ac.id()).startAt().plus(Duration.ofMinutes(25));
		assertThat(plan.get(knockout.id()).startAt()).isAfterOrEqualTo(poolEnd.plus(Duration.ofMinutes(20)));
	}

	@Test
	void startsEachEventAtItsSlotShiftedByTheOrganizer() {
		UUID later = new UUID(1, 2);
		PlanMatch morning = poolMatch("A", 1, entry(), entry());
		PlanMatch afternoon = new PlanMatch(new UUID(5, 99), later, MatchStatus.SCHEDULED, MatchPhase.POOL, 1, 99, "A",
				entry(), entry(), null, null, null, null);
		List<EventSetting> settings = List.of(new EventSetting(later, 2, null, List.of()));

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(morning, afternoon), List.of(COURT_1),
				settings, new PlanEvent(EVENT, null, null), new PlanEvent(later, at(240), null)));

		assertThat(plan.get(morning.id()).startAt()).isEqualTo(at(0));
		// Slot at 12:00, shifted by 2 slots of 25 min
		assertThat(plan.get(afternoon.id()).startAt()).isEqualTo(at(290));
	}

	@Test
	void holdsBackALaterStage() {
		PlanMatch quarter = match(MatchStatus.READY, MatchPhase.KNOCKOUT, 1, null, entry(), entry(), null, null);
		PlanMatch fin = match(MatchStatus.SCHEDULED, MatchPhase.KNOCKOUT, 2, null, null, entry(),
				MatchSource.winnerOf(quarter.id()), null);
		Instant nextMorning = START.plus(Duration.ofHours(25));
		List<EventSetting> settings = List.of(
				new EventSetting(EVENT, 0, null, List.of(new StageStart(ScheduleStage.FINAL, nextMorning))));

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(quarter, fin), List.of(COURT_1), settings));

		assertThat(plan.get(quarter.id()).startAt()).isEqualTo(at(0));
		assertThat(plan.get(fin.id()).startAt()).isEqualTo(nextMorning);
	}

	@Test
	void givesTheCourtsFirstToTheFirstEventOfTheOrganizer() {
		UUID series2 = new UUID(1, 2);
		PlanMatch series1Match = poolMatch("A", 1, entry(), entry());
		PlanMatch series2Match = new PlanMatch(new UUID(5, 98), series2, MatchStatus.SCHEDULED, MatchPhase.POOL, 1, 98,
				"A", entry(), entry(), null, null, null, null);
		List<EventSetting> settings = List.of(new EventSetting(series2, 0, null, List.of()),
				new EventSetting(EVENT, 0, null, List.of()));

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(series1Match, series2Match),
				List.of(COURT_1), settings, new PlanEvent(EVENT, null, null), new PlanEvent(series2, null, null)));

		assertThat(plan.get(series2Match.id()).startAt()).isEqualTo(at(0));
		assertThat(plan.get(series1Match.id()).startAt()).isEqualTo(at(25));
	}

	@Test
	void keepsTheMatchesOnACourtOrOverAndTheirPlayersBusy() {
		UUID teva = entry();
		PlanMatch onCourt = new PlanMatch(new UUID(5, 97), EVENT, MatchStatus.IN_PROGRESS, MatchPhase.POOL, 1, 97,
				"A", teva, entry(), null, null, COURT_1.id(), null);
		PlanMatch over = match(MatchStatus.FINISHED, MatchPhase.POOL, 1, "B", entry(), entry(), null, null);
		PlanMatch tevaNext = poolMatch("A", 2, teva, entry());
		PlanMatch free = poolMatch("B", 2, entry(), entry());

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input(List.of(onCourt, over, tevaNext, free),
				List.of(COURT_1, COURT_2), List.of()));

		assertThat(plan).doesNotContainKeys(onCourt.id(), over.id());
		// Court 1 is taken in the first slot, and Teva plays there until then
		assertThat(plan.get(free.id())).isEqualTo(new PlannedSlot(at(0), COURT_2.id()));
		assertThat(plan.get(tevaNext.id()).startAt()).isAfter(at(0));
	}

	@Test
	void plansNothingWithoutCourtOrMatch() {
		PlanMatch match = poolMatch("A", 1, entry(), entry());

		assertThat(SchedulePlanner.plan(input(List.of(match), List.of(), List.of()))).isEmpty();
		assertThat(SchedulePlanner.plan(input(List.of(), List.of(COURT_1), List.of()))).isEmpty();
	}

	@Test
	void warnsAboutEveryProblemOfTheSchedule() {
		UUID slotted = new UUID(1, 2);
		UUID teva = entry();
		PlanMatch noSlot = withStart(poolMatch("A", 1, entry(), entry()), at(0));
		PlanMatch first = withStart(new PlanMatch(new UUID(5, 90), slotted, MatchStatus.SCHEDULED, MatchPhase.POOL, 1,
				90, "A", teva, entry(), null, null, null, null), at(0));
		// Rest too short for Teva, and ends after the slot of the event (08:50)
		PlanMatch tooSoon = withStart(new PlanMatch(new UUID(5, 91), slotted, MatchStatus.SCHEDULED, MatchPhase.POOL,
				2, 91, "A", teva, entry(), null, null, null, null), at(30));
		// Placed before its source match ends
		PlanMatch dependent = withStart(new PlanMatch(new UUID(5, 92), slotted, MatchStatus.SCHEDULED,
				MatchPhase.KNOCKOUT, 1, 92, null, null, entry(), MatchSource.winnerOf(first.id()), null, null, null),
				at(10));
		PlanMatch unplanned = poolMatch("B", 1, entry(), entry());

		List<ScheduleWarning> warnings = SchedulePlanner.check(input(
				List.of(noSlot, first, tooSoon, dependent, unplanned), List.of(COURT_1), List.of(),
				new PlanEvent(EVENT, null, null), new PlanEvent(slotted, at(0), at(50))));

		assertThat(warnings).containsExactlyInAnyOrder(ScheduleWarning.ofEvent("NO_SLOT", EVENT, null),
				ScheduleWarning.ofEvent("OUTSIDE_SLOT", slotted, 1), ScheduleWarning.ofMatch("REST_TOO_SHORT", tooSoon.id()),
				ScheduleWarning.ofMatch("ORDER", dependent.id()), ScheduleWarning.ofEvent("UNSCHEDULED", null, 1));
	}

	@Test
	void hasNoWarningForTheScheduleItComputes() {
		UUID a = entry();
		UUID b = entry();
		UUID c = entry();
		List<PlanMatch> matches = List.of(poolMatch("A", 1, a, b), poolMatch("A", 2, b, c), poolMatch("A", 3, a, c));
		Input input = input(matches, List.of(COURT_1, COURT_2), List.of(), new PlanEvent(EVENT, START, null));

		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input);
		List<PlanMatch> placed = matches.stream().map((match) -> planned(match, plan)).toList();

		assertThat(SchedulePlanner.check(input(placed, List.of(COURT_1, COURT_2), List.of(),
				new PlanEvent(EVENT, START, null)))).isEmpty();
	}

	private static PlanMatch withStart(PlanMatch match, Instant start) {
		return new PlanMatch(match.id(), match.eventId(), match.status(), match.phase(), match.round(), match.number(),
				match.pool(), match.sideA(), match.sideB(), match.sourceA(), match.sourceB(), match.courtId(), start);
	}

}
