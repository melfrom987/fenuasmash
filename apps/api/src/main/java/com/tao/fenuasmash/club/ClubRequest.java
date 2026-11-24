package com.tao.fenuasmash.club;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ClubRequest(@NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 100) String city,
		@Size(max = 200) String address, @Email @Size(max = 254) String email, @Size(max = 30) String phone) {
}
