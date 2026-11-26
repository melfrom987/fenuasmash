package com.tao.fenuasmash.draw;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.event.EventResponse;
import com.tao.fenuasmash.event.EventService;
import com.tao.fenuasmash.match.MatchService;
import com.tao.fenuasmash.player.PlayerRankings;
import com.tao.fenuasmash.player.PlayerSummary;
import com.tao.fenuasmash.player.Ranking;
import com.tao.fenuasmash.registration.RegistrationResponse;
import com.tao.fenuasmash.registration.RegistrationService;
import com.tao.fenuasmash.registration.RegistrationStatus;
import com.tao.fenuasmash.tournament.Discipline;
import com.tao.fenuasmash.tournament.DrawFormat;
import com.tao.fenuasmash.tournament.TournamentService;

/**
 * The draw of an event: run on its confirmed registrations, changed while it is a draft, then
 * published. See "Pools and Draw" in `docs/competition-rules.md`.
 */
@Service
@Transactional(readOnly = true)
public class DrawService {

	private final DrawRepository repository;

	private final EventService eventService;

	private final TournamentService tournamentService;

	private final RegistrationService registrationService;

	private final MatchService matchService;

	private final Clock clock = Clock.systemUTC();

	DrawService(DrawRepository repository, EventService eventService, TournamentService tournamentService,
			RegistrationService registrationService, MatchService matchService) {
		this.repository = repository;
		this.eventService = eventService;
		this.tournamentService = tournamentService;
		this.registrationService = registrationService;
		this.matchService = matchService;
	}

	DrawResponse find(UUID eventId) {
		EventResponse event = eventService.findById(eventId);
		return toResponse(event, getDraw(eventId));
	}

	/**
	 * Draws the confirmed registrations of the event, replacing the draft. A missing setting
	 * uses the event value, then the default; without a random seed, a new one is drawn.
	 */
	@Transactional
	DrawResponse create(UUID eventId, DrawSettingsRequest settings) {
		EventResponse event = eventService.findById(eventId);
		Draw draw = repository.findById(eventId).orElseGet(() -> new Draw(eventId));
		if (draw.getStatus() == DrawStatus.PUBLISHED) {
			throw DrawLock.published();
		}
		List<DrawEntrant> entrants = registrationService.findAllByEvent(eventId)
			.stream()
			.filter((registration) -> registration.status() == RegistrationStatus.CONFIRMED)
			.map((registration) -> entrant(registration, event.discipline()))
			.toList();
		DrawOptions requested = new DrawOptions(settings.poolCount(), settings.qualifiersPerPool(),
				settings.seedCount());
		DrawOptions options = new DrawOptions(
				(requested.poolCount() != null) ? requested.poolCount() : event.poolCount(),
				(requested.qualifiersPerPool() != null) ? requested.qualifiersPerPool() : event.qualifiersPerPool(),
				requested.seedCount());
		long randomSeed = (settings.randomSeed() != null) ? settings.randomSeed() : DrawAlgorithm.newRandomSeed();
		DrawPlan plan = DrawAlgorithm.draw(tournamentService.findById(event.tournamentId()).drawFormat(), entrants,
				options, randomSeed);
		draw.replace(plan, requested, clock.instant());
		return toResponse(event, repository.save(draw));
	}

	@Transactional
	void delete(UUID eventId) {
		eventService.findById(eventId);
		Draw draw = getDraw(eventId);
		if (draw.getStatus() == DrawStatus.PUBLISHED) {
			throw DrawLock.published();
		}
		repository.delete(draw);
	}

	/**
	 * Swaps two entries between pools or within a pool, or two lines of the bracket. A pool slot
	 * cannot be swapped with a bracket line.
	 */
	@Transactional
	DrawResponse swap(UUID eventId, DrawSwapRequest request) {
		EventResponse event = eventService.findById(eventId);
		Draw draw = getDraw(eventId);
		if (draw.getStatus() == DrawStatus.PUBLISHED) {
			throw DrawLock.published();
		}
		DrawSwapRequest.SlotRef a = request.a();
		DrawSwapRequest.SlotRef b = request.b();
		if (a.isPoolSlot() && b.isPoolSlot()) {
			draw.swapPoolEntries(poolEntryIndex(draw, a), poolEntryIndex(draw, b));
		}
		else if (a.isBracketLine() && b.isBracketLine()) {
			draw.swapBracketLines(bracketIndex(draw, a), bracketIndex(draw, b));
		}
		else {
			throw invalidSwap("Swap two pool slots ({pool, index}) or two bracket lines ({position})");
		}
		return toResponse(event, draw);
	}

	/**
	 * Locks the draw, and with it the registrations of the event. A single elimination gets
	 * the matches of its whole bracket (pools come with #93).
	 */
	@Transactional
	DrawResponse publish(UUID eventId) {
		EventResponse event = eventService.findById(eventId);
		Draw draw = getDraw(eventId);
		if (draw.getStatus() == DrawStatus.PUBLISHED) {
			throw DrawLock.published();
		}
		draw.publish(clock.instant());
		if (tournamentService.findById(event.tournamentId()).drawFormat() == DrawFormat.SINGLE_ELIMINATION) {
			List<UUID> lines = draw.getBracket().stream().map(BracketRow::registrationId).toList();
			matchService.createKnockout(eventId, KnockoutPlanner.plan(lines));
		}
		return toResponse(event, draw);
	}

	/** Index in all the pool entries of the draw of {@code ref}. */
	private static int poolEntryIndex(Draw draw, DrawSwapRequest.SlotRef ref) {
		int inPool = 0;
		List<PoolEntryRow> entries = draw.getPoolEntries();
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).pool().equals(ref.pool())) {
				if (inPool == ref.index()) {
					return i;
				}
				inPool++;
			}
		}
		throw invalidSwap("Pool " + ref.pool() + " has no slot " + ref.index());
	}

	private static int bracketIndex(Draw draw, DrawSwapRequest.SlotRef ref) {
		if (ref.position() < 1 || ref.position() > draw.getBracket().size()) {
			throw invalidSwap("The bracket has no line " + ref.position());
		}
		return ref.position() - 1;
	}

	private static DrawEntrant entrant(RegistrationResponse registration, Discipline discipline) {
		Set<String> clubs = new HashSet<>();
		clubs.add(registration.player().clubName());
		if (registration.partner() != null) {
			clubs.add(registration.partner().clubName());
		}
		clubs.remove(null);
		Integer points = points(registration, discipline);
		return new DrawEntrant(registration.id(), (points != null) ? points.doubleValue() : null,
				registration.registeredAt(), clubs);
	}

	/** Ranking points in the discipline, the sum for a pair; null when neither is ranked. */
	private static Integer points(RegistrationResponse registration, Discipline discipline) {
		Integer player = ranking(registration.player().rankings(), discipline).points();
		Integer partner = (registration.partner() != null)
				? ranking(registration.partner().rankings(), discipline).points() : null;
		if (player == null && partner == null) {
			return null;
		}
		return ((player != null) ? player : 0) + ((partner != null) ? partner : 0);
	}

	private static Ranking ranking(PlayerRankings rankings, Discipline discipline) {
		return switch (discipline) {
			case MS, WS -> rankings.singles();
			case MD, WD, OD -> rankings.doubles();
			case XD -> rankings.mixed();
		};
	}

	private DrawResponse toResponse(EventResponse event, Draw draw) {
		List<RegistrationResponse> registrations = registrationService.findAllByEvent(event.id());
		Map<UUID, RegistrationResponse> byId = registrations.stream()
			.collect(Collectors.toMap(RegistrationResponse::id, Function.identity()));
		List<DrawResponse.Pool> pools = new ArrayList<>();
		for (PoolRow pool : draw.getPools()) {
			List<DrawResponse.Entry> entries = draw.getPoolEntries()
				.stream()
				.filter((entry) -> entry.pool().equals(pool.name()))
				.map((entry) -> entry(byId.get(entry.registrationId()), entry.registrationId(), entry.seed(),
						event.discipline()))
				.toList();
			pools.add(new DrawResponse.Pool(pool.name(), pool.qualifiers(), entries));
		}
		DrawResponse.Bracket bracket = null;
		if (draw.getBracketSize() != null) {
			List<DrawResponse.Slot> slots = new ArrayList<>();
			List<BracketRow> rows = draw.getBracket();
			for (int i = 0; i < rows.size(); i++) {
				BracketRow row = rows.get(i);
				DrawResponse.Entry entry = (row.registrationId() != null)
						? entry(byId.get(row.registrationId()), row.registrationId(), row.seed(), event.discipline())
						: null;
				DrawResponse.Qualifier qualifier = (row.qualifierPool() != null)
						? new DrawResponse.Qualifier(row.qualifierPool(), row.qualifierRank()) : null;
				slots.add(new DrawResponse.Slot(i + 1, entry, qualifier, row.bye()));
			}
			bracket = new DrawResponse.Bracket(draw.getBracketSize(), slots);
		}
		List<UUID> excluded = registrations.stream()
			.filter((registration) -> registration.status() != RegistrationStatus.CONFIRMED)
			.map(RegistrationResponse::id)
			.toList();
		return new DrawResponse(draw.getEventId(), draw.getStatus(), draw.getSeedCount(), pools, bracket, excluded,
				draw.getRandomSeed(), draw.getCreatedAt(), draw.getPublishedAt());
	}

	private static DrawResponse.Entry entry(RegistrationResponse registration, UUID registrationId, Integer seed,
			Discipline discipline) {
		if (registration == null) {
			// Cannot happen: a change to the registrations deletes the draft (DrawLock)
			throw new IllegalStateException("Registration " + registrationId + " of a draw is missing");
		}
		return new DrawResponse.Entry(registrationId, seed, points(registration, discipline), registration.player(),
				registration.partner());
	}

	private Draw getDraw(UUID eventId) {
		return repository.findById(eventId).orElseThrow(() -> new NotFoundException("Draw", eventId));
	}

	private static InvalidRequestException invalidSwap(String message) {
		return new InvalidRequestException("INVALID_SWAP", message);
	}

}
