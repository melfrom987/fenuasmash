package com.tao.fenuasmash.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tao.fenuasmash.common.security.IssuedToken;
import com.tao.fenuasmash.common.security.JwtService;
import com.tao.fenuasmash.common.security.TokenClaims;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final UserAccountRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	/**
	 * Checked when the email is unknown, so that a login takes about as long
	 * whether the account exists or not.
	 */
	private final String unknownAccountHash;

	AuthService(UserAccountRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.unknownAccountHash = passwordEncoder.encode("unknown-account-password");
	}

	public LoginResponse login(LoginRequest request) {
		UserAccount account = repository.findByEmail(UserAccount.normalizeEmail(request.email())).orElse(null);
		String hash = account != null ? account.getPasswordHash() : unknownAccountHash;
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
		if (account == null || !passwordMatches) {
			throw new InvalidCredentialsException();
		}
		IssuedToken token = jwtService
			.issue(new TokenClaims(account.getId(), account.getRole(), account.getOrganizationId()));
		return new LoginResponse(token.value(), "Bearer", token.expiresAt());
	}

	/**
	 * Reads the account from the database rather than from the token, so that the
	 * answer is up to date.
	 */
	public CurrentUserResponse currentUser(TokenClaims claims) {
		return repository.findById(claims.userId())
			.map(CurrentUserResponse::from)
			.orElseThrow(UnknownAccountException::new);
	}

}
