package com.escapehub.mission.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Η εξωτερική πηγή ενός γρίφου. Η απάντηση ελέγχεται πάντα με την τιμή του αρχείου, ποτέ με τη ζωντανή απόκριση
 * (ADR-0002).
 *
 * @param provider {@code wikidata} ή {@code openlibrary}
 * @param snapshot το αποθηκευμένο αντίγραφο της απόκρισης, σχετικά με το {@code snapshots/}
 */
public record DataSource(
		@NotNull @Pattern(regexp = DataSource.PROVIDERS, message = "επιτρέπεται wikidata ή openlibrary")
		String provider,
		@NotBlank String snapshot) {

	static final String PROVIDERS = "wikidata|openlibrary";

}
