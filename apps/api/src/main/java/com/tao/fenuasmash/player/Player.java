package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tao.fenuasmash.common.text.TextKeys;

@Entity
@Table(name = "player")
public class Player {

	@Id
	private UUID id;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Gender gender;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "club_id", nullable = false)
	private UUID clubId;

	@Column(name = "license_number")
	private String licenseNumber;

	private String email;

	private String phone;

	@Column(name = "singles_points")
	private Integer singlesPoints;

	@Column(name = "singles_position")
	private Integer singlesPosition;

	@Column(name = "doubles_points")
	private Integer doublesPoints;

	@Column(name = "doubles_position")
	private Integer doublesPosition;

	@Column(name = "mixed_points")
	private Integer mixedPoints;

	@Column(name = "mixed_position")
	private Integer mixedPosition;

	/**
	 * Names in both orders and license number, without case or accents, to search on any of
	 * them.
	 */
	@Column(name = "search_key", nullable = false)
	private String searchKey;

	protected Player() {
	}

	Player(PlayerDetails details) {
		this.id = UUID.randomUUID();
		update(details);
	}

	void update(PlayerDetails details) {
		this.firstName = details.firstName();
		this.lastName = details.lastName();
		this.gender = details.gender();
		this.birthDate = details.birthDate();
		this.clubId = details.clubId();
		this.licenseNumber = details.licenseNumber();
		this.email = details.email();
		this.phone = details.phone();
		this.singlesPoints = details.rankings().singles().points();
		this.singlesPosition = details.rankings().singles().position();
		this.doublesPoints = details.rankings().doubles().points();
		this.doublesPosition = details.rankings().doubles().position();
		this.mixedPoints = details.rankings().mixed().points();
		this.mixedPosition = details.rankings().mixed().position();
		String license = (licenseNumber != null) ? " " + licenseNumber : "";
		this.searchKey = TextKeys.of(lastName + " " + firstName + " | " + firstName + " " + lastName + license);
	}

	PlayerDetails details() {
		return new PlayerDetails(firstName, lastName, gender, birthDate, clubId, licenseNumber, email, phone,
				rankings());
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Gender getGender() {
		return gender;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public UUID getClubId() {
		return clubId;
	}

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public PlayerRankings rankings() {
		return new PlayerRankings(new Ranking(singlesPoints, singlesPosition),
				new Ranking(doublesPoints, doublesPosition), new Ranking(mixedPoints, mixedPosition));
	}

}
