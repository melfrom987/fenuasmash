package com.tao.fenuasmash.tablet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import com.tao.fenuasmash.common.security.TabletPrincipal;

/**
 * Creates and hashes pairing codes and tablet tokens.
 * <p>
 * Both are random and only their SHA-256 hash is stored. Unlike passwords, they
 * are not chosen by people, so a fast hash is enough: a token has 256 random
 * bits, and a code is only valid for a few minutes.
 */
final class TabletSecrets {

	/** No 0, O, 1, I or L: easy to read on a screen and type on a tablet. */
	static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

	static final int CODE_LENGTH = 6;

	private static final int TOKEN_BYTES = 32;

	private static final SecureRandom RANDOM = new SecureRandom();

	private TabletSecrets() {
	}

	static String newCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
		}
		return code.toString();
	}

	/** Upper case, without spaces and dashes, as umpires may type them. */
	static String normalizeCode(String code) {
		return code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
	}

	static String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return TabletPrincipal.TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256, as 64 lower-case hexadecimal characters. */
	static String hash(String secret) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

}
