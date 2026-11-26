package com.escapehub.game.application;

/**
 * Το αποτέλεσμα της εξέτασης ενός αντικειμένου.
 *
 * @param item η καταχώριση του στοιχείου στο ημερολόγιο
 * @param added {@code true} αν προστέθηκε τώρα, {@code false} αν ήταν ήδη στο ημερολόγιο (US-07, AC3)
 */
public record ExamineResult(JournalItem item, boolean added) {
}
