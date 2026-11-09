package com.tao.fenuasmash.court;

import jakarta.validation.constraints.NotNull;

public record UpdateCourtRequest(@NotNull Boolean active) {
}
