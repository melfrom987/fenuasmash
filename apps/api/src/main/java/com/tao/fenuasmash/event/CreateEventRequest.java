package com.tao.fenuasmash.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(@NotBlank @Size(max = 100) String name, @NotNull EventType type) {
}
