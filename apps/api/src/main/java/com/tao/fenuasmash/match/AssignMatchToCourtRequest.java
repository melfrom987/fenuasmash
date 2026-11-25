package com.tao.fenuasmash.match;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignMatchToCourtRequest(@NotNull UUID courtId, @NotNull @Min(0) Integer stateVersion) {
}
