package com.tao.fenuasmash.tablet;

import java.time.Instant;
import java.util.UUID;

/**
 * A new pairing code, in clear: the only time it is shown.
 */
public record PairingCodeResponse(String code, UUID courtId, Instant expiresAt) {
}
