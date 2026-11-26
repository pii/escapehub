package com.escapehub.user.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Τα tokens που αναγνωρίζουν έναν παίκτη μέσω cookie. Στη βάση αποθηκεύεται μόνο το SHA-256 του token.
 */
final class PlayerTokens {

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final int TOKEN_BYTES = 32;

	private PlayerTokens() {
	}

	static String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Το SHA-256 είναι διαθέσιμο σε κάθε JVM", ex);
		}
	}

}
