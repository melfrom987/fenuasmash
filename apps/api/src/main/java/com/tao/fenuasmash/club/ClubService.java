package com.tao.fenuasmash.club;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.FieldValidationException;
import com.tao.fenuasmash.common.error.NotFoundException;
import com.tao.fenuasmash.common.text.TextKeys;

@Service
public class ClubService {

	private final ClubRepository repository;

	ClubService(ClubRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	List<ClubResponse> findAll(String query) {
		List<Club> clubs = (query == null || query.isBlank()) ? repository.findAllByOrderByNameKeyAsc()
				: repository.findAllByNameKeyContainingOrderByNameKeyAsc(TextKeys.of(query));
		return clubs.stream().map(ClubResponse::from).toList();
	}

	@Transactional(readOnly = true)
	ClubResponse findById(UUID id) {
		return ClubResponse.from(getClub(id));
	}

	@Transactional
	ClubResponse create(ClubRequest request) {
		String name = request.name().strip();
		if (repository.existsByNameKey(TextKeys.of(name))) {
			throw duplicateName(name);
		}
		return ClubResponse.from(repository.save(new Club(name, request.city().strip(), blankToNull(request.address()),
				blankToNull(request.email()), blankToNull(request.phone()))));
	}

	@Transactional
	ClubResponse update(UUID id, UpdateClubRequest request) {
		Club club = getClub(id);
		String name = club.getName();
		if (request.nameSent()) {
			name = required("name", request.name());
			if (repository.existsByNameKeyAndIdNot(TextKeys.of(name), id)) {
				throw duplicateName(name);
			}
		}
		String city = request.citySent() ? required("city", request.city()) : club.getCity();
		String address = request.addressSent() ? blankToNull(request.address()) : club.getAddress();
		String email = request.emailSent() ? blankToNull(request.email()) : club.getEmail();
		String phone = request.phoneSent() ? blankToNull(request.phone()) : club.getPhone();
		club.update(name, city, address, email, phone);
		return ClubResponse.from(club);
	}

	/** The club as shown inside a player, or empty if it does not exist. */
	@Transactional(readOnly = true)
	public Optional<ClubSummary> findSummary(UUID id) {
		return repository.findById(id).map(ClubSummary::from);
	}

	/** The clubs among {@code ids}, by id; unknown ids are left out. */
	@Transactional(readOnly = true)
	public Map<UUID, ClubSummary> findSummaries(Collection<UUID> ids) {
		return repository.findAllById(ids)
			.stream()
			.map(ClubSummary::from)
			.collect(Collectors.toMap(ClubSummary::id, Function.identity()));
	}

	private Club getClub(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Club", id));
	}

	/** A field that cannot be cleared: sent as null, it is refused. */
	private static String required(String field, String value) {
		if (value == null || value.isBlank()) {
			throw new FieldValidationException(field, "must not be blank");
		}
		return value.strip();
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static ConflictException duplicateName(String name) {
		return new ConflictException("CLUB_NAME_ALREADY_EXISTS", "A club named '" + name + "' already exists");
	}

}
