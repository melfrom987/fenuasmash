package com.tao.fenuasmash.player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.club.ClubService;
import com.tao.fenuasmash.club.ClubSummary;
import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.FieldValidationException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.common.text.TextKeys;

@Service
public class PlayerService {

	static final int MAX_PAGE_SIZE = 100;

	private static final Sort BY_NAME = Sort.by("lastName", "firstName", "id");

	private final PlayerRepository repository;

	private final ClubService clubService;

	PlayerService(PlayerRepository repository, ClubService clubService) {
		this.repository = repository;
		this.clubService = clubService;
	}

	@Transactional(readOnly = true)
	PlayerPage search(String query, UUID clubId, Gender gender, int page, int size) {
		if (page < 0) {
			throw new FieldValidationException("page", "must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new FieldValidationException("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		Specification<Player> matches = (root, criteriaQuery, builder) -> builder.conjunction();
		if (query != null && !query.isBlank()) {
			String key = "%" + escapeLike(TextKeys.of(query)) + "%";
			matches = matches.and((root, criteriaQuery, builder) -> builder.like(root.get("searchKey"), key, '\\'));
		}
		if (clubId != null) {
			matches = matches.and((root, criteriaQuery, builder) -> builder.equal(root.get("clubId"), clubId));
		}
		if (gender != null) {
			matches = matches.and((root, criteriaQuery, builder) -> builder.equal(root.get("gender"), gender));
		}
		Page<Player> players = repository.findAll(matches, PageRequest.of(page, size, BY_NAME));
		Map<UUID, ClubSummary> clubs = clubService
			.findSummaries(players.stream().map(Player::getClubId).distinct().toList());
		List<PlayerResponse> items = players.stream()
			.map((player) -> PlayerResponse.from(player, clubs.get(player.getClubId())))
			.toList();
		return new PlayerPage(items, page, size, players.getTotalElements());
	}

	@Transactional(readOnly = true)
	PlayerResponse findById(UUID id) {
		return toResponse(getPlayer(id));
	}

	@Transactional
	PlayerResponse create(CreatePlayerRequest request) {
		String licenseNumber = blankToNull(request.licenseNumber());
		if (licenseNumber != null && repository.existsByLicenseNumber(licenseNumber)) {
			throw duplicateLicense(licenseNumber);
		}
		PlayerDetails details = new PlayerDetails(request.firstName().strip(), request.lastName().strip(),
				request.gender(), request.birthDate(), checkClub(request.clubId()), licenseNumber,
				blankToNull(request.email()), blankToNull(request.phone()),
				(request.rankings() != null) ? request.rankings() : PlayerRankings.NONE);
		return toResponse(repository.save(new Player(details)));
	}

	@Transactional
	PlayerResponse update(UUID id, UpdatePlayerRequest request) {
		Player player = getPlayer(id);
		PlayerDetails current = player.details();
		String licenseNumber = current.licenseNumber();
		if (request.licenseNumberSent()) {
			licenseNumber = blankToNull(request.licenseNumber());
			if (licenseNumber != null && repository.existsByLicenseNumberAndIdNot(licenseNumber, id)) {
				throw duplicateLicense(licenseNumber);
			}
		}
		PlayerDetails details = new PlayerDetails(
				request.firstNameSent() ? required("firstName", request.firstName()) : current.firstName(),
				request.lastNameSent() ? required("lastName", request.lastName()) : current.lastName(),
				request.genderSent() ? required("gender", request.gender()) : current.gender(),
				request.birthDateSent() ? required("birthDate", request.birthDate()) : current.birthDate(),
				request.clubIdSent() ? checkClub(required("clubId", request.clubId())) : current.clubId(),
				licenseNumber, request.emailSent() ? blankToNull(request.email()) : current.email(),
				request.phoneSent() ? blankToNull(request.phone()) : current.phone(),
				request.rankingsSent() ? required("rankings", request.rankings()) : current.rankings());
		player.update(details);
		return toResponse(player);
	}

	private PlayerResponse toResponse(Player player) {
		ClubSummary club = clubService.findSummary(player.getClubId())
			.orElseThrow(() -> new IllegalStateException("Club " + player.getClubId() + " of a player is missing"));
		return PlayerResponse.from(player, club);
	}

	private UUID checkClub(UUID clubId) {
		if (clubService.findSummary(clubId).isEmpty()) {
			throw new InvalidRequestException("INVALID_CLUB", "Club " + clubId + " does not exist");
		}
		return clubId;
	}

	private Player getPlayer(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Player", id));
	}

	/** A field that cannot be cleared: sent as null, it is refused. */
	@SuppressWarnings("unchecked")
	private static <T> T required(String field, T value) {
		if (value == null) {
			throw new FieldValidationException(field, "must not be null");
		}
		return (value instanceof String text) ? (T) text.strip() : value;
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static ConflictException duplicateLicense(String licenseNumber) {
		return new ConflictException("PLAYER_LICENSE_ALREADY_EXISTS",
				"A player with license number '" + licenseNumber + "' already exists");
	}

}
