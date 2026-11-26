package com.escapehub.team;

/**
 * Το αναγνωριστικό μιας ομάδας παικτών. Το χρησιμοποιούν τα άλλα modules για να αναφερθούν σε ομάδα.
 *
 * @param value θετικός αριθμός, το πρωτεύον κλειδί της ομάδας στη βάση
 */
public record TeamId(long value) {

	public TeamId {
		if (value <= 0) {
			throw new IllegalArgumentException("Το TeamId πρέπει να είναι θετικό: " + value);
		}
	}

}
