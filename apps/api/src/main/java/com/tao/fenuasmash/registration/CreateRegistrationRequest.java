package com.tao.fenuasmash.registration;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record CreateRegistrationRequest(@NotNull UUID playerId, UUID partnerId) {
}
