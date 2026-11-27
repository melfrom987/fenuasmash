package com.tao.fenuasmash.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.court.CourtResponse;
import com.tao.fenuasmash.court.CourtService;
import com.tao.fenuasmash.event.EventResponse;
import com.tao.fenuasmash.event.EventService;
import com.tao.fenuasmash.match.MatchResponse;
import com.tao.fenuasmash.match.MatchService;
import com.tao.fenuasmash.match.PlannedSlot;
import com.tao.fenuasmash.registration.RegistrationResponse;
import com.tao.fenuasmash.registration.RegistrationService;
import com.tao.fenuasmash.schedule.SchedulePlanner.EventSetting;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanCourt;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanEvent;
import com.tao.fenuasmash.schedule.SchedulePlanner.PlanMatch;
import com.tao.fenuasmash.tournament.ScheduleState;
import com.tao.fenuasmash.tournament.ScheduleStatus;
import com.tao.fenuasmash.tournament.TournamentResponse;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * The schedule of a tournament: computed from the published draws ({@link SchedulePlanner}),
 * adjusted by the organizer, then published. The planned places are on the matches.
 */
@Service
@Transactional(readOnly = true)
public class ScheduleService {

	private final TournamentService tournamentService;

	private final EventService eventService;

	private final CourtService courtService;

	private final RegistrationService registrationService;

	private final MatchService matchService;

	private final ScheduleEventSettingRepository settings;

	ScheduleService(TournamentService tournamentService, EventService eventService, CourtService courtService,
			RegistrationService registrationService, MatchService matchService,
			ScheduleEventSettingRepository settings) {
		this.tournamentService = tournamentService;
		this.eventService = eventService;
		this.courtService = courtService;
		this.registrationService = registrationService;
		this.matchService = matchService;
		this.settings = settings;
	}

	/** The state of the schedule, its events and its warnings, computed on each read. */
	ScheduleResponse get(UUID tournamentId) {
		ScheduleState state = tournamentService.findScheduleState(tournamentId);
		return toResponse(tournamentId, state, input(tournamentId));
	}

	/**
	 * Plans every match not started yet, and sets the schedule back to a draft.
	 * @param request event order and shifts, saved for the next computations; null keeps the
	 * saved ones
	 * @throws ConflictException `NO_ACTIVE_COURT`, or `NO_MATCHES_TO_SCHEDULE`
	 */
	@Transactional
	ScheduleResponse generate(UUID tournamentId, ScheduleRequests.Generate request) {
		tournamentService.findById(tournamentId);
		if (request != null && request.events() != null) {
			saveSettings(tournamentId, request.events());
		}
		SchedulePlanner.Input input = input(tournamentId);
		if (input.courts().stream().noneMatch(PlanCourt::active)) {
			throw new ConflictException("NO_ACTIVE_COURT", "Tournament " + tournamentId + " has no active court");
		}
		if (input.matches().stream().noneMatch((match) -> MatchService.PLANNABLE.contains(match.status()))) {
			throw new ConflictException("NO_MATCHES_TO_SCHEDULE",
					"Tournament " + tournamentId + " has no match to schedule: publish a draw first");
		}
		Map<UUID, PlannedSlot> plan = SchedulePlanner.plan(input);
		matchService.replacePlan(tournamentId, plan);
		ScheduleState state = tournamentService.markScheduleGenerated(tournamentId, Instant.now());
		return toResponse(tournamentId, state, input(tournamentId));
	}

	/**
	 * Moves a match not started yet, swapping with a match not started yet at that place.
	 * @throws ConflictException `SCHEDULE_NOT_GENERATED`, `MATCH_ALREADY_STARTED`,
	 * `COURT_INACTIVE` or `SLOT_TAKEN`
	 */
	@Transactional
	ScheduleResponse move(UUID tournamentId, ScheduleRequests.Move request) {
		ScheduleState state = generatedState(tournamentId);
		matchService.moveInSchedule(tournamentId, request.matchId(), request.plannedStartAt(), request.courtId());
		return toResponse(tournamentId, state, input(tournamentId));
	}

	/** @throws ConflictException `SCHEDULE_NOT_GENERATED` */
	@Transactional
	ScheduleResponse publish(UUID tournamentId) {
		generatedState(tournamentId);
		ScheduleState state = tournamentService.markSchedulePublished(tournamentId, Instant.now());
		return toResponse(tournamentId, state, input(tournamentId));
	}

	private ScheduleState generatedState(UUID tournamentId) {
		ScheduleState state = tournamentService.findScheduleState(tournamentId);
		if (state.status() == ScheduleStatus.NOT_GENERATED) {
			throw new ConflictException("SCHEDULE_NOT_GENERATED",
					"The schedule of tournament " + tournamentId + " is not computed yet");
		}
		return state;
	}

	private void saveSettings(UUID tournamentId, List<ScheduleRequests.EventSetting> requested) {
		Set<UUID> events = new HashSet<>();
		eventService.findAllByTournament(tournamentId).forEach((event) -> events.add(event.id()));
		Set<UUID> seen = new HashSet<>();
		List<ScheduleEventSetting> rows = new ArrayList<>();
		for (ScheduleRequests.EventSetting setting : requested) {
			if (!events.contains(setting.eventId()) || !seen.add(setting.eventId())) {
				throw invalid("Event " + setting.eventId() + " is not an event of the tournament, or appears twice");
			}
			List<ScheduleRequests.StageStart> stages = Objects.requireNonNullElse(setting.stageStarts(), List.of());
			if (stages.stream().map(ScheduleRequests.StageStart::stage).distinct().count() != stages.size()) {
				throw invalid("A stage appears twice for event " + setting.eventId());
			}
			rows.add(new ScheduleEventSetting(setting.eventId(), tournamentId, rows.size(), setting.offsetSlots(),
					setting.startAt(),
					stages.stream().map((stage) -> new StageStartRow(stage.stage(), stage.at())).toList()));
		}
		settings.deleteAllOfTournament(tournamentId);
		settings.saveAllAndFlush(rows);
	}

	private SchedulePlanner.Input input(UUID tournamentId) {
		TournamentResponse tournament = tournamentService.findById(tournamentId);
		List<EventResponse> events = eventService.findAllByTournament(tournamentId);
		Map<UUID, List<UUID>> players = new HashMap<>();
		for (EventResponse event : events) {
			for (RegistrationResponse registration : registrationService.findAllByEvent(event.id())) {
				List<UUID> ids = new ArrayList<>();
				ids.add(registration.player().id());
				if (registration.partner() != null) {
					ids.add(registration.partner().id());
				}
				players.put(registration.id(), ids);
			}
		}
		List<CourtResponse> courts = courtService.findAllByTournament(tournamentId);
		List<PlanMatch> matches = matchService.findAllByTournament(tournamentId, Set.of())
			.stream()
			.map(ScheduleService::planMatch)
			.toList();
		List<EventSetting> saved = settings.findAllByTournamentIdOrderByPosition(tournamentId)
			.stream()
			.map((row) -> new EventSetting(row.getEventId(), row.getOffsetSlots(), row.getStartAt(),
					row.getStageStarts()
						.stream()
						.map((stage) -> new SchedulePlanner.StageStart(stage.stage(), stage.startsAt()))
						.toList()))
			.toList();
		return new SchedulePlanner.Input(tournament.startsAt(), tournament.averageMatchMinutes(),
				tournament.minimumRestMinutes(),
				events.stream().map((event) -> new PlanEvent(event.id(), event.plannedDate(), event.plannedEndDate()))
					.toList(),
				courts.stream().map((court) -> new PlanCourt(court.id(), court.number(), court.active())).toList(),
				players, matches, saved);
	}

	private static PlanMatch planMatch(MatchResponse match) {
		return new PlanMatch(match.id(), match.eventId(), match.status(), match.phase(), match.round(), match.number(),
				match.pool(), match.sideA(), match.sideB(), match.sourceA(), match.sourceB(), match.courtId(),
				match.plannedStartAt());
	}

	private static ScheduleResponse toResponse(UUID tournamentId, ScheduleState state, SchedulePlanner.Input input) {
		Duration duration = Duration.ofMinutes(input.averageMatchMinutes());
		Map<UUID, EventSetting> byEvent = new HashMap<>();
		Map<UUID, Integer> priority = new HashMap<>();
		for (EventSetting setting : input.settings()) {
			byEvent.put(setting.eventId(), setting);
			priority.put(setting.eventId(), priority.size());
		}
		long tournamentStart = input.tournamentStart().toEpochMilli();
		List<ScheduleResponse.Event> events = input.events()
			.stream()
			.filter((event) -> input.matches().stream().anyMatch((match) -> match.eventId().equals(event.id())))
			.sorted(Comparator.<PlanEvent>comparingInt((event) -> priority.getOrDefault(event.id(), Integer.MAX_VALUE))
				.thenComparingLong((event) -> SchedulePlanner.eventStart(event, byEvent.get(event.id()),
						tournamentStart, duration.toMillis())))
			.map((event) -> eventOf(event, byEvent.get(event.id()), input.matches(), duration))
			.toList();
		List<Instant> starts = input.matches()
			.stream()
			.map(PlanMatch::plannedStartAt)
			.filter(Objects::nonNull)
			.sorted()
			.toList();
		List<ScheduleWarning> warnings = (state.status() == ScheduleStatus.NOT_GENERATED) ? List.of()
				: SchedulePlanner.check(input);
		return new ScheduleResponse(tournamentId, state.status(), events, state.generatedAt(), state.publishedAt(),
				starts.isEmpty() ? null : starts.getFirst(),
				starts.isEmpty() ? null : starts.getLast().plus(duration), warnings);
	}

	private static ScheduleResponse.Event eventOf(PlanEvent event, EventSetting setting, List<PlanMatch> matches,
			Duration duration) {
		List<PlanMatch> own = matches.stream().filter((match) -> match.eventId().equals(event.id())).toList();
		List<Instant> starts = own.stream().map(PlanMatch::plannedStartAt).filter(Objects::nonNull).sorted().toList();
		List<ScheduleResponse.StageStart> stages = (setting != null) ? setting.stageStarts()
			.stream()
			.map((stage) -> new ScheduleResponse.StageStart(stage.stage(), stage.at()))
			.toList() : List.of();
		return new ScheduleResponse.Event(event.id(), (setting != null) ? setting.offsetSlots() : 0,
				(setting != null) ? setting.startAt() : null, stages, own.size(),
				starts.isEmpty() ? null : starts.getFirst(), starts.isEmpty() ? null : starts.getLast().plus(duration));
	}

	private static InvalidRequestException invalid(String message) {
		return new InvalidRequestException("VALIDATION_FAILED", message);
	}

}
