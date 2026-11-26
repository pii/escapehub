package com.escapehub.mission.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Ο ορισμός μιας αποστολής, όπως διαβάζεται από ένα αρχείο του {@code missions/} (ADR-0001). Η μορφή του αρχείου
 * περιγράφεται στο {@code docs/mission-format.md}.
 *
 * <p>Καθαρή Java χωρίς Spring, γιατί το μοντέλο το χρησιμοποιεί και ο μηχανισμός του {@code game.domain}. Οι
 * annotations του Bean Validation ελέγχουν μόνο τη δομή. Τις αναφορές, τους κύκλους και την ολοκλήρωση τα ελέγχει
 * ο {@link MissionValidator}.
 *
 * @param id μοναδικό και σταθερό αναγνωριστικό, π.χ. {@code villa-diodati}
 * @param version αυξάνεται σε κάθε αλλαγή του περιεχομένου, ώστε ένα playthrough να ξέρει με ποια έκδοση ξεκίνησε
 * @param title ο τίτλος στον κατάλογο
 * @param summary σύντομη περιγραφή για τον κατάλογο
 * @param minPlayers ελάχιστος αριθμός παικτών
 * @param estimatedMinutes εκτιμώμενη διάρκεια σε λεπτά
 * @param paths οι διαδρομές, που εξερευνώνται με οποιαδήποτε σειρά
 * @param evidence τα στοιχεία που μπαίνουν στο κοινό ημερολόγιο
 * @param puzzles οι γρίφοι, μαζί με τον τελικό
 */
public record Mission(
		@NotNull @Pattern(regexp = Mission.ID_PATTERN, message = Mission.ID_MESSAGE) String id,
		@Positive int version,
		@NotBlank String title,
		@NotBlank String summary,
		@Positive int minPlayers,
		@Positive int estimatedMinutes,
		@NotEmpty List<@NotNull @Valid MissionPath> paths,
		@NotEmpty List<@NotNull @Valid Evidence> evidence,
		@NotEmpty List<@NotNull @Valid Puzzle> puzzles) {

	/**
	 * Η μορφή κάθε id του αρχείου: πεζά λατινικά, ψηφία και παύλες.
	 */
	public static final String ID_PATTERN = "[a-z0-9]+(-[a-z0-9]+)*";

	static final String ID_MESSAGE = "επιτρέπονται μόνο πεζά λατινικά, ψηφία και παύλες, π.χ. letter-1816";

	/**
	 * Αμετάβλητα αντίγραφα. Μια λίστα που λείπει από το αρχείο γίνεται κενή. Δεν χρησιμοποιούμε το
	 * {@code List.copyOf}, γιατί απορρίπτει ένα κενό στοιχείο λίστας με {@code NullPointerException}, πριν το
	 * Bean Validation προλάβει να δείξει το πεδίο.
	 */
	public Mission {
		paths = Collections.unmodifiableList(new ArrayList<>((paths != null) ? paths : List.of()));
		evidence = Collections.unmodifiableList(new ArrayList<>((evidence != null) ? evidence : List.of()));
		puzzles = Collections.unmodifiableList(new ArrayList<>((puzzles != null) ? puzzles : List.of()));
	}

	/**
	 * Τα στοιχεία που είναι διαθέσιμα από την αρχή ({@code initial: true}), χωρίς να λυθεί κάποιος γρίφος.
	 */
	public List<Evidence> initialEvidence() {
		return evidence.stream().filter(Evidence::initial).toList();
	}

	public Optional<Evidence> findEvidence(String evidenceId) {
		return evidence.stream().filter(item -> item.id().equals(evidenceId)).findFirst();
	}

	public Optional<Puzzle> findPuzzle(String puzzleId) {
		return puzzles.stream().filter(puzzle -> puzzle.id().equals(puzzleId)).findFirst();
	}

}
