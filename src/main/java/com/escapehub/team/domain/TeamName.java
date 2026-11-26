package com.escapehub.team.domain;

import com.escapehub.common.BadRequestException;

/**
 * Το όνομα μιας ομάδας: υποχρεωτικό, έως {@value #MAX_LENGTH} χαρακτήρες.
 *
 * @param value το όνομα χωρίς κενά στην αρχή και στο τέλος
 */
public record TeamName(String value) {

	public static final int MAX_LENGTH = 40;

	public TeamName {
		if (value == null || value.isBlank() || value.length() > MAX_LENGTH || !value.equals(value.strip())) {
			throw new BadRequestException(
					"Το όνομα της ομάδας είναι υποχρεωτικό και έχει έως 40 χαρακτήρες.");
		}
	}

	/**
	 * Δημιουργεί όνομα από ό,τι έγραψε ο παίκτης, αφαιρώντας τα κενά στην αρχή και στο τέλος.
	 */
	public static TeamName of(String raw) {
		return new TeamName(raw == null ? null : raw.strip());
	}

}
