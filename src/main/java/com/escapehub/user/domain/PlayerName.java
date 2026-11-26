package com.escapehub.user.domain;

import java.util.regex.Pattern;

import com.escapehub.common.BadRequestException;

/**
 * Το όνομα ενός παίκτη: 3–20 χαρακτήρες, μόνο γράμματα (και ελληνικά), ψηφία, {@code _} και {@code -}.
 *
 * @param value το όνομα χωρίς κενά στην αρχή και στο τέλος
 */
public record PlayerName(String value) {

	private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{Nd}_-]{3,20}");

	public PlayerName {
		if (value == null || !ALLOWED.matcher(value).matches()) {
			throw new BadRequestException(
					"Το όνομα πρέπει να έχει 3–20 χαρακτήρες: γράμματα, ψηφία, _ ή -.");
		}
	}

	/**
	 * Δημιουργεί όνομα από ό,τι έγραψε ο παίκτης, αφαιρώντας τα κενά στην αρχή και στο τέλος.
	 */
	public static PlayerName of(String raw) {
		return new PlayerName(raw == null ? null : raw.strip());
	}

}
