package com.tao.fenuasmash.registration;

import java.util.UUID;

/**
 * A class rather than a record: a setter is only called when its field is present in the
 * JSON body, which tells an absent partner (kept) from a null one (removed).
 */
public class UpdateRegistrationRequest {

	private UUID partnerId;

	private boolean partnerIdSent;

	private Boolean confirm;

	public void setPartnerId(UUID partnerId) {
		this.partnerId = partnerId;
		this.partnerIdSent = true;
	}

	public void setConfirm(Boolean confirm) {
		this.confirm = confirm;
	}

	UUID partnerId() {
		return partnerId;
	}

	boolean partnerIdSent() {
		return partnerIdSent;
	}

	boolean confirm() {
		return Boolean.TRUE.equals(confirm);
	}

}
