package com.escapehub.journal.application;

import java.time.Instant;

import com.escapehub.user.PlayerId;

/**
 * Μια καταχώριση του ημερολογίου: ποιο στοιχείο, ποιος το βρήκε και πότε.
 *
 * @param evidenceId το id του στοιχείου στο αρχείο της αποστολής
 * @param addedBy ο παίκτης που το πρόσθεσε πρώτος
 * @param addedAt πότε προστέθηκε
 */
public record JournalEntry(String evidenceId, PlayerId addedBy, Instant addedAt) {
}
