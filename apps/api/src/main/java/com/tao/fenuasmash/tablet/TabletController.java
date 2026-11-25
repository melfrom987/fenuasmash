package com.tao.fenuasmash.tablet;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tao.fenuasmash.common.security.TabletPrincipal;

@RestController
@RequestMapping("/api")
class TabletController {

	private final TabletService service;

	TabletController(TabletService service) {
		this.service = service;
	}

	/**
	 * No {@code Location} header: the code is shown once and cannot be read
	 * again.
	 */
	@PostMapping("/courts/{courtId}/pairing-code")
	ResponseEntity<PairingCodeResponse> createPairingCode(@PathVariable UUID courtId) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.cacheControl(CacheControl.noStore())
			.body(service.createPairingCode(courtId));
	}

	@PostMapping("/tablets/pair")
	ResponseEntity<PairTabletResponse> pair(@Valid @RequestBody PairTabletRequest request,
			HttpServletRequest httpRequest) {
		// Behind the web proxy, the client IP comes from X-Forwarded-For (server.forward-headers-strategy)
		PairTabletResponse response = service.pair(request, httpRequest.getRemoteAddr());
		// The token is a credential: no cache may keep it
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
	}

	@GetMapping("/tablets/me")
	CurrentTabletResponse me(@AuthenticationPrincipal TabletPrincipal principal) {
		return service.currentTablet(principal);
	}

	@GetMapping("/tournaments/{tournamentId}/tablets")
	List<TabletResponse> findAllByTournament(@PathVariable UUID tournamentId) {
		return service.findAllByTournament(tournamentId);
	}

	@PostMapping("/tablets/{id}/revoke")
	TabletResponse revoke(@PathVariable UUID id) {
		return service.revoke(id);
	}

}
