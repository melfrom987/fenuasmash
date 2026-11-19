package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.util.UUID;

public record TabletResponse(UUID id, UUID courtId, int courtNumber, String deviceName, Instant pairedAt,
		Instant lastSeenAt, Instant revokedAt) {

	static TabletResponse from(Tablet tablet, int courtNumber) {
		return new TabletResponse(tablet.getId(), tablet.getCourtId(), courtNumber, tablet.getDeviceName(),
				tablet.getPairedAt(), tablet.getLastSeenAt(), tablet.getRevokedAt());
	}

}
