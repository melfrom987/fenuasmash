package com.tao.fenuasmash.tablet;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.common.security.InvalidTokenException;
import com.tao.fenuasmash.common.security.TabletAuthenticator;
import com.tao.fenuasmash.common.security.TabletPrincipal;
import com.tao.fenuasmash.court.CourtResponse;
import com.tao.fenuasmash.court.CourtService;

/**
 * Pairs umpire tablets with courts and checks their tokens.
 * <p>
 * The organizer creates a one-time code for a court; the umpire types it on the
 * tablet, which gets a token in exchange. A court has one paired tablet at
 * most: pairing a new one revokes the previous one. A revoked tablet can no
 * longer call the API.
 */
@Service
@Transactional(readOnly = true)
public class TabletService implements TabletAuthenticator {

	static final Duration CODE_TTL = Duration.ofMinutes(10);

	static final String TABLET_REVOKED_CODE = "TABLET_REVOKED";

	/** A new code that matches a code still in use is drawn again. */
	private static final int MAX_CODE_DRAWS = 20;

	private final TabletRepository tabletRepository;

	private final PairingCodeRepository codeRepository;

	private final CourtService courtService;

	private final PairingAttemptLimiter attemptLimiter;

	TabletService(TabletRepository tabletRepository, PairingCodeRepository codeRepository,
			CourtService courtService, PairingAttemptLimiter attemptLimiter) {
		this.tabletRepository = tabletRepository;
		this.codeRepository = codeRepository;
		this.courtService = courtService;
		this.attemptLimiter = attemptLimiter;
	}

	/**
	 * Creates the code of a court, replacing the previous one.
	 */
	@Transactional
	public PairingCodeResponse createPairingCode(UUID courtId) {
		CourtResponse court = courtService.findById(courtId);
		checkActive(court);
		Instant now = Instant.now();
		codeRepository.deleteExpired(now);
		codeRepository.deleteByCourtId(courtId);
		for (int draw = 0; draw < MAX_CODE_DRAWS; draw++) {
			String code = TabletSecrets.newCode();
			String codeHash = TabletSecrets.hash(code);
			if (!codeRepository.existsByCodeHash(codeHash)) {
				PairingCode saved = codeRepository.save(new PairingCode(courtId, codeHash, now, now.plus(CODE_TTL)));
				return new PairingCodeResponse(code, courtId, saved.getExpiresAt());
			}
		}
		throw new IllegalStateException("Could not draw a free pairing code");
	}

	/**
	 * Uses a code to pair a tablet with its court, and returns the tablet token.
	 */
	@Transactional
	public PairTabletResponse pair(PairTabletRequest request, String clientIp) {
		attemptLimiter.checkAllowed(clientIp);
		Instant now = Instant.now();
		String codeHash = TabletSecrets.hash(TabletSecrets.normalizeCode(request.code()));
		PairingCode code = codeRepository.findByCodeHash(codeHash).filter(found -> !found.isExpired(now)).orElse(null);
		// Deleting the code makes it single use, even when two tablets send it at the same time
		if (code == null || codeRepository.deleteByCourtIdAndCodeHash(code.getCourtId(), codeHash) == 0) {
			attemptLimiter.recordFailure(clientIp);
			throw new InvalidRequestException("INVALID_PAIRING_CODE", "Unknown, expired or used pairing code");
		}
		CourtResponse court = courtService.findById(code.getCourtId());
		checkActive(court);
		tabletRepository.revokeActiveByCourtId(court.id(), now);
		String token = TabletSecrets.newToken();
		Tablet tablet = tabletRepository
			.save(new Tablet(court.id(), normalizeDeviceName(request.deviceName()), TabletSecrets.hash(token), now));
		return new PairTabletResponse(tablet.getId(), token, "Bearer", court);
	}

	/**
	 * Called by the security filter on every request sent with a tablet token:
	 * also records when the tablet was last seen.
	 */
	@Override
	@Transactional
	public TabletPrincipal authenticate(String token) {
		Tablet tablet = tabletRepository.findByTokenHash(TabletSecrets.hash(token))
			.orElseThrow(() -> new InvalidTokenException("Unknown tablet token"));
		if (tablet.isRevoked()) {
			throw new InvalidTokenException("Tablet " + tablet.getId() + " is revoked", TABLET_REVOKED_CODE);
		}
		tablet.recordActivity(Instant.now());
		return new TabletPrincipal(tablet.getId(), tablet.getCourtId());
	}

	public CurrentTabletResponse currentTablet(TabletPrincipal principal) {
		Tablet tablet = getTablet(principal.tabletId());
		return new CurrentTabletResponse(tablet.getId(), tablet.getDeviceName(), tablet.getPairedAt(),
				courtService.findById(tablet.getCourtId()));
	}

	/**
	 * The tablets of a tournament, by court number, then the most recent pairing
	 * first.
	 */
	public List<TabletResponse> findAllByTournament(UUID tournamentId) {
		Map<UUID, Integer> courtNumbers = courtService.findAllByTournament(tournamentId)
			.stream()
			.collect(Collectors.toMap(CourtResponse::id, CourtResponse::number));
		if (courtNumbers.isEmpty()) {
			return List.of();
		}
		return tabletRepository.findAllByCourtIdIn(courtNumbers.keySet())
			.stream()
			.map(tablet -> TabletResponse.from(tablet, courtNumbers.get(tablet.getCourtId())))
			.sorted(Comparator.comparingInt(TabletResponse::courtNumber)
				.thenComparing(TabletResponse::pairedAt, Comparator.reverseOrder()))
			.toList();
	}

	@Transactional
	public TabletResponse revoke(UUID id) {
		Tablet tablet = getTablet(id);
		tablet.revoke(Instant.now());
		return TabletResponse.from(tablet, courtService.findById(tablet.getCourtId()).number());
	}

	private Tablet getTablet(UUID id) {
		return tabletRepository.findById(id).orElseThrow(() -> new NotFoundException("Tablet", id));
	}

	private static void checkActive(CourtResponse court) {
		if (!court.active()) {
			throw new ConflictException("COURT_INACTIVE", "Court " + court.id() + " is inactive");
		}
	}

	private static String normalizeDeviceName(String deviceName) {
		if (deviceName == null || deviceName.isBlank()) {
			return null;
		}
		return deviceName.strip();
	}

}
