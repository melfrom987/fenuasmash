package com.tao.fenuasmash.player;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CreatePlayerRequest(@NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
		@NotNull Gender gender, @NotNull LocalDate birthDate, @NotNull UUID clubId,
		@Size(max = 30) String licenseNumber, @Email @Size(max = 254) String email, @Size(max = 30) String phone,
		@Valid PlayerRankings rankings) {
}
