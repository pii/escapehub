package com.escapehub.mission.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Ένα πεδίο που συμπληρώνει ο παίκτης σε γρίφο {@link PuzzleType#COMBINATION}. Οι αποδεκτές τιμές είναι στο
 * {@link Answer#values()}.
 */
public record PuzzleField(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		@NotBlank String title) {
}
