package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A class rather than a record: a setter is only called when its field is present in the
 * JSON body, which tells an absent field (kept) from a null one (cleared).
 */
public class UpdatePlayerRequest {

	@Size(min = 1, max = 100)
	@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
	private String firstName;

	private boolean firstNameSent;

	@Size(min = 1, max = 100)
	@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
	private String lastName;

	private boolean lastNameSent;

	private Gender gender;

	private boolean genderSent;

	private LocalDate birthDate;

	private boolean birthDateSent;

	private UUID clubId;

	private boolean clubIdSent;

	@Size(max = 30)
	private String licenseNumber;

	private boolean licenseNumberSent;

	@Email
	@Size(max = 254)
	private String email;

	private boolean emailSent;

	@Size(max = 30)
	private String phone;

	private boolean phoneSent;

	@Valid
	private PlayerRankings rankings;

	private boolean rankingsSent;

	public void setFirstName(String firstName) {
		this.firstName = firstName;
		this.firstNameSent = true;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
		this.lastNameSent = true;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
		this.genderSent = true;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
		this.birthDateSent = true;
	}

	public void setClubId(UUID clubId) {
		this.clubId = clubId;
		this.clubIdSent = true;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
		this.licenseNumberSent = true;
	}

	public void setEmail(String email) {
		this.email = email;
		this.emailSent = true;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		this.phoneSent = true;
	}

	public void setRankings(PlayerRankings rankings) {
		this.rankings = rankings;
		this.rankingsSent = true;
	}

	String firstName() {
		return firstName;
	}

	boolean firstNameSent() {
		return firstNameSent;
	}

	String lastName() {
		return lastName;
	}

	boolean lastNameSent() {
		return lastNameSent;
	}

	Gender gender() {
		return gender;
	}

	boolean genderSent() {
		return genderSent;
	}

	LocalDate birthDate() {
		return birthDate;
	}

	boolean birthDateSent() {
		return birthDateSent;
	}

	UUID clubId() {
		return clubId;
	}

	boolean clubIdSent() {
		return clubIdSent;
	}

	String licenseNumber() {
		return licenseNumber;
	}

	boolean licenseNumberSent() {
		return licenseNumberSent;
	}

	String email() {
		return email;
	}

	boolean emailSent() {
		return emailSent;
	}

	String phone() {
		return phone;
	}

	boolean phoneSent() {
		return phoneSent;
	}

	PlayerRankings rankings() {
		return rankings;
	}

	boolean rankingsSent() {
		return rankingsSent;
	}

}
