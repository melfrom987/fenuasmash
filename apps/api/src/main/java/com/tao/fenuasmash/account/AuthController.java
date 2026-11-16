package com.tao.fenuasmash.account;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tao.fenuasmash.common.security.TokenClaims;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AuthService service;

	AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/login")
	ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		// The token is a credential: no cache may keep it
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(request));
	}

	@GetMapping("/me")
	ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal TokenClaims claims) {
		// Personal data: no shared cache may keep it
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.currentUser(claims));
	}

}
