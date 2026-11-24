package com.tao.fenuasmash.tablet;

import java.util.UUID;

import com.tao.fenuasmash.court.CourtResponse;

/**
 * The tablet token, in clear: the only time it is sent.
 */
public record PairTabletResponse(UUID tabletId, String accessToken, String tokenType, CourtResponse court) {
}
