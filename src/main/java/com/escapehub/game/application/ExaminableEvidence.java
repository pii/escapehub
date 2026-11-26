package com.escapehub.game.application;

/**
 * Ένα αντικείμενο που μπορεί να εξετάσει η ομάδα (US-07).
 *
 * @param evidenceId το id του στοιχείου
 * @param title ό,τι βλέπουν οι παίκτες
 * @param pathTitle η διαδρομή όπου βρίσκεται
 * @param inJournal {@code true} αν το στοιχείο είναι ήδη στο ημερολόγιο
 */
public record ExaminableEvidence(String evidenceId, String title, String pathTitle, boolean inJournal) {
}
