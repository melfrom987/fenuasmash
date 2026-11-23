package com.tao.fenuasmash.account;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.error.ConflictException;
import com.tao.fenuasmash.common.error.FieldValidationException;
import com.tao.fenuasmash.common.error.InvalidRequestException;
import com.tao.fenuasmash.organization.OrganizationService;

@Service
@Transactional(readOnly = true)
public class AccountService {

	/**
	 * Club accounts will come with the club records, as each one is linked to its
	 * club; player accounts come with online registration.
	 */
	private static final Set<UserRole> CREATABLE_ROLES = Set.of(UserRole.ORGANIZER);

	/** bcrypt ignores everything after 72 bytes, so longer passwords are refused. */
	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserAccountRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final OrganizationService organizationService;

	AccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
			OrganizationService organizationService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.organizationService = organizationService;
	}

	@Transactional
	public AccountResponse create(CreateAccountRequest request) {
		if (!CREATABLE_ROLES.contains(request.role())) {
			throw new InvalidRequestException("INVALID_ROLE", "Only ORGANIZER accounts can be created for now");
		}
		if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new FieldValidationException("password", "must be at most " + MAX_PASSWORD_BYTES + " bytes");
		}
		checkOrganization(request);
		String email = UserAccount.normalizeEmail(request.email());
		if (repository.existsByEmail(email)) {
			throw new ConflictException("EMAIL_ALREADY_USED", "An account already uses this email");
		}
		UserAccount account = repository.save(new UserAccount(email, passwordEncoder.encode(request.password()),
				request.role(), request.organizationId()));
		return AccountResponse.from(account);
	}

	private void checkOrganization(CreateAccountRequest request) {
		if (request.role() != UserRole.ORGANIZER) {
			if (request.organizationId() != null) {
				throw new FieldValidationException("organizationId", "must be null for this role");
			}
			return;
		}
		if (request.organizationId() == null) {
			throw new FieldValidationException("organizationId", "must not be null for an organizer");
		}
		if (!organizationService.exists(request.organizationId())) {
			throw new InvalidRequestException("INVALID_ORGANIZATION",
					"Organization " + request.organizationId() + " does not exist");
		}
	}

}
