package com.tao.fenuasmash.registration;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

record TransferRequest(@NotEmpty List<@NotNull UUID> registrationIds) {
}
