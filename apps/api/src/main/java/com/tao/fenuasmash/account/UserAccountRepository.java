package com.tao.fenuasmash.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	/**
	 * @param email an email already normalized with {@link UserAccount#normalizeEmail}
	 */
	Optional<UserAccount> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByRole(UserRole role);

}
