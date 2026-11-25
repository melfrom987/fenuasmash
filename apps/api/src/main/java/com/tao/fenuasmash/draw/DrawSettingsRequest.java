package com.tao.fenuasmash.draw;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** The `DrawSettings` schema: all optional, null for the event value, then the default. */
record DrawSettingsRequest(@Min(1) @Max(16) Integer poolCount, @Min(1) @Max(7) Integer qualifiersPerPool,
		@Min(0) Integer seedCount, @Min(0) @Max(DrawAlgorithm.MAX_RANDOM_SEED) Long randomSeed) {
}
