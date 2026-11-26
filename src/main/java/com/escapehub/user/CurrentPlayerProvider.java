package com.escapehub.user;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Βρίσκει ποιος παίκτης έκανε ένα αίτημα.
 *
 * <p>Η πραγματική υλοποίηση, με αναγνώριση από cookie, είναι το US-01. Μέχρι τότε δεν υπάρχει bean και κάθε
 * {@link CurrentPlayer} δίνει 401. Στους ελέγχους χρησιμοποιείται το {@code TestPlayerConfiguration}.
 */
public interface CurrentPlayerProvider {

	Optional<PlayerId> currentPlayer(HttpServletRequest request);

}
