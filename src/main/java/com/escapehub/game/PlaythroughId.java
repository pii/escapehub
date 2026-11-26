package com.escapehub.game;

/**
 * Το αναγνωριστικό ενός playthrough. Το χρησιμοποιούν τα άλλα modules, π.χ. το ημερολόγιο (#28).
 *
 * @param value θετικός αριθμός, το πρωτεύον κλειδί του playthrough στη βάση
 */
public record PlaythroughId(long value) {

	public PlaythroughId {
		if (value <= 0) {
			throw new IllegalArgumentException("Το PlaythroughId πρέπει να είναι θετικό: " + value);
		}
	}

}
