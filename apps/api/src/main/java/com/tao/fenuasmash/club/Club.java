package com.tao.fenuasmash.club;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tao.fenuasmash.common.text.TextKeys;

@Entity
@Table(name = "club")
public class Club {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	/** The name without case or accents: unique. */
	@Column(name = "name_key", nullable = false)
	private String nameKey;

	@Column(nullable = false)
	private String city;

	private String address;

	private String email;

	private String phone;

	protected Club() {
	}

	Club(String name, String city, String address, String email, String phone) {
		this.id = UUID.randomUUID();
		update(name, city, address, email, phone);
	}

	void update(String name, String city, String address, String email, String phone) {
		this.name = name;
		this.nameKey = TextKeys.of(name);
		this.city = city;
		this.address = address;
		this.email = email;
		this.phone = phone;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCity() {
		return city;
	}

	public String getAddress() {
		return address;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

}
