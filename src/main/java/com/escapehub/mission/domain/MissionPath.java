package com.escapehub.mission.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Μια διαδρομή της αποστολής, π.χ. «Η επιστολή». Οι διαδρομές εξερευνώνται με οποιαδήποτε σειρά.
 */
public record MissionPath(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		@NotBlank String title) {
}
