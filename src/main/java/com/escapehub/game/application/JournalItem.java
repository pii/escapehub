package com.escapehub.game.application;

import java.time.Instant;

import com.escapehub.user.PlayerId;

/**
 * Μια καταχώριση του ημερολογίου όπως τη βλέπουν τα μέλη της ομάδας (US-07).
 *
 * @param evidenceId το id του στοιχείου
 * @param title ο τίτλος του στοιχείου
 * @param addedBy ο παίκτης που το βρήκε
 * @param addedByName το όνομα του παίκτη
 * @param addedAt πότε το βρήκε
 */
public record JournalItem(String evidenceId, String title, PlayerId addedBy, String addedByName,
		Instant addedAt) {
}
