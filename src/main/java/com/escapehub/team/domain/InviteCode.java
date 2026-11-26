package com.escapehub.team.domain;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Κωδικός πρόσκλησης σε ομάδα: {@value #LENGTH} χαρακτήρες από αλφάβητο χωρίς χαρακτήρες που μπερδεύονται
 * (0/O, 1/I/L).
 */
public final class InviteCode {

	public static final int LENGTH = 8;

	private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

	private static final Pattern VALID = Pattern.compile("[" + ALPHABET + "]{" + LENGTH + "}");

	private static final SecureRandom RANDOM = new SecureRandom();

	private InviteCode() {
	}

	/**
	 * Νέος τυχαίος κωδικός.
	 */
	public static String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

	/**
	 * Ο κωδικός όπως τον έγραψε ο παίκτης, χωρίς κενά και με κεφαλαία. Κενό αν δεν έχει τη μορφή κωδικού.
	 */
	public static Optional<String> normalize(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		String code = raw.strip().toUpperCase(Locale.ROOT);
		return VALID.matcher(code).matches() ? Optional.of(code) : Optional.empty();
	}

}
