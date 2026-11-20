package com.tao.fenuasmash.match;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScoreEventBatchRequest(
		@NotNull @Size(min = 1, max = 100) List<@Valid @NotNull ScoreEventRequest> events) {
}
