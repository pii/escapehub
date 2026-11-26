package com.escapehub.mission.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ένας γρίφος της αποστολής.
 *
 * <p>Κανόνας ξεκλειδώματος: ο γρίφος ανοίγει όταν υπάρχουν στο ημερολόγιο <b>όλα</b> τα στοιχεία του
 * {@code unlockedBy}. Κενή λίστα σημαίνει ότι είναι ανοιχτός από την αρχή (ADR-0001).
 *
 * @param id μοναδικό στην αποστολή
 * @param path η διαδρομή του γρίφου. Μπορεί να λείπει, π.χ. στον τελικό γρίφο.
 * @param type ο τύπος, που ορίζει ποια πεδία χρειάζονται
 * @param title ο τίτλος του γρίφου
 * @param prompt η εκφώνηση που βλέπουν οι παίκτες
 * @param unlockedBy τα στοιχεία που χρειάζονται, όλα μαζί, για να ανοίξει
 * @param reveals τα στοιχεία που προστίθενται στο ημερολόγιο όταν λυθεί
 * @param actions οι ενέργειες ενός {@link PuzzleType#SEQUENCE}
 * @param contributors πόσοι διαφορετικοί παίκτες πρέπει να απαντήσουν σε {@link PuzzleType#COOPERATIVE}
 * @param fields τα πεδία ενός {@link PuzzleType#COMBINATION}
 * @param source η εξωτερική πηγή, αν ο γρίφος χρησιμοποιεί δημόσια δεδομένα
 * @param answer η σωστή απάντηση. Διαβάζεται από το αρχείο, αλλά δεν γράφεται ποτέ σε JSON (WRITE_ONLY).
 * @param completesMission {@code true} μόνο για τον τελικό γρίφο
 */
public record Puzzle(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		String path,
		@NotNull PuzzleType type,
		@NotBlank String title,
		String prompt,
		List<@NotNull String> unlockedBy,
		List<@NotNull String> reveals,
		List<@NotNull @Valid PuzzleAction> actions,
		Integer contributors,
		List<@NotNull @Valid PuzzleField> fields,
		@Valid DataSource source,
		@NotNull @Valid @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) Answer answer,
		boolean completesMission) {

	/**
	 * Αμετάβλητα αντίγραφα, όπως στο {@link Mission}. Μια λίστα που λείπει γίνεται κενή.
	 */
	public Puzzle {
		unlockedBy = Collections.unmodifiableList(
				new ArrayList<>((unlockedBy != null) ? unlockedBy : List.of()));
		reveals = Collections.unmodifiableList(new ArrayList<>((reveals != null) ? reveals : List.of()));
		actions = Collections.unmodifiableList(new ArrayList<>((actions != null) ? actions : List.of()));
		fields = Collections.unmodifiableList(new ArrayList<>((fields != null) ? fields : List.of()));
	}

}
