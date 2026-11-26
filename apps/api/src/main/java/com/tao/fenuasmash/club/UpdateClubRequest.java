package com.tao.fenuasmash.club;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A class rather than a record: a setter is only called when its field is present in the
 * JSON body, which tells an absent field (kept) from a null one (cleared).
 */
public class UpdateClubRequest {

	@Size(min = 1, max = 100)
	@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
	private String name;

	private boolean nameSent;

	@Size(min = 1, max = 100)
	@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
	private String city;

	private boolean citySent;

	@Size(max = 200)
	private String address;

	private boolean addressSent;

	@Email
	@Size(max = 254)
	private String email;

	private boolean emailSent;

	@Size(max = 30)
	private String phone;

	private boolean phoneSent;

	public void setName(String name) {
		this.name = name;
		this.nameSent = true;
	}

	public void setCity(String city) {
		this.city = city;
		this.citySent = true;
	}

	public void setAddress(String address) {
		this.address = address;
		this.addressSent = true;
	}

	public void setEmail(String email) {
		this.email = email;
		this.emailSent = true;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		this.phoneSent = true;
	}

	String name() {
		return name;
	}

	boolean nameSent() {
		return nameSent;
	}

	String city() {
		return city;
	}

	boolean citySent() {
		return citySent;
	}

	String address() {
		return address;
	}

	boolean addressSent() {
		return addressSent;
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

}
