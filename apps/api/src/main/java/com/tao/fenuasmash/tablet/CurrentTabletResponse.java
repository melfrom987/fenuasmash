package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.util.UUID;

import com.tao.fenuasmash.court.CourtResponse;

public record CurrentTabletResponse(UUID id, String deviceName, Instant pairedAt, CourtResponse court) {
}
