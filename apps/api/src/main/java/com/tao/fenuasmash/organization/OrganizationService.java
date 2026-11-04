package com.tao.fenuasmash.organization;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.NotFoundException;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

	private final OrganizationRepository repository;

	OrganizationService(OrganizationRepository repository) {
		this.repository = repository;
	}

	public List<OrganizationResponse> findAll() {
		return repository.findAll(Sort.by("name")).stream().map(OrganizationResponse::from).toList();
	}

	public OrganizationResponse findById(UUID id) {
		return repository.findById(id)
			.map(OrganizationResponse::from)
			.orElseThrow(() -> new NotFoundException("Organization", id));
	}

	@Transactional
	public OrganizationResponse create(CreateOrganizationRequest request) {
		Organization organization = repository.save(new Organization(request.name().strip()));
		return OrganizationResponse.from(organization);
	}

}
