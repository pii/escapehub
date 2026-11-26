package com.escapehub.user;

/**
 * Το αναγνωριστικό ενός παίκτη. Το χρησιμοποιούν όλα τα modules για να αναφερθούν σε παίκτη.
 *
 * @param value θετικός αριθμός, το πρωτεύον κλειδί του παίκτη στη βάση
 */
public record PlayerId(long value) {

	public PlayerId {
		if (value <= 0) {
			throw new IllegalArgumentException("Το PlayerId πρέπει να είναι θετικό: " + value);
		}
	}

}
