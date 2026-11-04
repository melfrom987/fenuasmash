package com.tao.fenuasmash.organization;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
class OrganizationController {

	private final OrganizationService service;

	OrganizationController(OrganizationService service) {
		this.service = service;
	}

	@GetMapping
	List<OrganizationResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	OrganizationResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping
	ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
		OrganizationResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/organizations/" + created.id())).body(created);
	}

}
