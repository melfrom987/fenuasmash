package com.tao.fenuasmash.event;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
		@Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String name,
		EventType type) {
}
