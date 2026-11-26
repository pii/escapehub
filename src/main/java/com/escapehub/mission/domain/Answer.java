package com.escapehub.mission.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Η σωστή απάντηση ενός γρίφου. Μένει στο backend: δεν γράφεται σε JSON (βλ. {@link Puzzle#answer()}) και το
 * {@link #toString()} δεν τη δείχνει, π.χ. σε logs ή σε template.
 *
 * <p>Κάθε τύπος γρίφου χρησιμοποιεί άλλα πεδία. Ο {@link MissionValidator} ελέγχει ότι υπάρχουν τα σωστά.
 *
 * @param accepted αποδεκτές απαντήσεις για {@link PuzzleType#TEXT_ANSWER} και {@link PuzzleType#COOPERATIVE}
 * @param steps τα id των ενεργειών με τη σωστή σειρά, για {@link PuzzleType#SEQUENCE}
 * @param evidence τα στοιχεία που πρέπει να επιλεγούν, για {@link PuzzleType#COMBINATION}
 * @param values οι αποδεκτές τιμές κάθε πεδίου, για {@link PuzzleType#COMBINATION}
 */
public record Answer(
		List<@NotBlank String> accepted,
		List<@NotNull String> steps,
		List<@NotNull String> evidence,
		Map<@NotNull String, @NotNull List<@NotBlank String>> values) {

	/**
	 * Αμετάβλητα αντίγραφα, όπως στο {@link Mission}. Ό,τι λείπει γίνεται κενό.
	 */
	public Answer {
		accepted = Collections.unmodifiableList(new ArrayList<>((accepted != null) ? accepted : List.of()));
		steps = Collections.unmodifiableList(new ArrayList<>((steps != null) ? steps : List.of()));
		evidence = Collections.unmodifiableList(new ArrayList<>((evidence != null) ? evidence : List.of()));
		values = Collections.unmodifiableMap(copy(values));
	}

	private static Map<String, List<String>> copy(Map<String, List<String>> values) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		if (values != null) {
			values.forEach((field, accepted) -> copy.put(field, (accepted != null)
					? Collections.unmodifiableList(new ArrayList<>(accepted)) : null));
		}
		return copy;
	}

	@Override
	public String toString() {
		return "Answer[κρυφή]";
	}

}
