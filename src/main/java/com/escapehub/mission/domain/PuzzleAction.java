package com.escapehub.mission.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Μια ενέργεια που μπορεί να διαλέξει ο παίκτης σε γρίφο {@link PuzzleType#SEQUENCE}. Η σωστή σειρά είναι στο
 * {@link Answer#steps()}.
 */
public record PuzzleAction(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		@NotBlank String title) {
}
