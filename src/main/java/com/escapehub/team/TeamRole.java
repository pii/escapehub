package com.escapehub.team;

/**
 * Ο ρόλος ενός παίκτη σε μια ομάδα. Ο δημιουργός είναι αυτός που ξεκινά τις αποστολές της ομάδας (US-06).
 */
public enum TeamRole {

	CREATOR("Δημιουργός"),

	MEMBER("Μέλος");

	private final String label;

	TeamRole(String label) {
		this.label = label;
	}

	/**
	 * Το όνομα του ρόλου όπως εμφανίζεται στο UI.
	 */
	public String label() {
		return label;
	}

}
