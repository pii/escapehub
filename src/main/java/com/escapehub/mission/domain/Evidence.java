package com.escapehub.mission.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Ένα στοιχείο που μπαίνει στο κοινό ημερολόγιο.
 *
 * @param id το id που χρησιμοποιούν τα {@code unlockedBy} και {@code reveals} των γρίφων
 * @param path η διαδρομή όπου βρίσκεται
 * @param title ό,τι βλέπουν οι παίκτες στο ημερολόγιο
 * @param initial {@code true} αν είναι διαθέσιμο από την αρχή. Αλλιώς το αποκαλύπτει ένας γρίφος.
 */
public record Evidence(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		@NotNull String path,
		@NotBlank String title,
		boolean initial) {
}
