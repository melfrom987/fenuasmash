package com.tao.fenuasmash.tablet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param code the code given by the organizer, case insensitive, spaces and
 * dashes ignored
 * @param deviceName optional, to help the organizer recognize the tablet
 */
public record PairTabletRequest(
		@NotBlank @Size(max = 20) String code,
		@Size(max = 100) String deviceName) {
}
