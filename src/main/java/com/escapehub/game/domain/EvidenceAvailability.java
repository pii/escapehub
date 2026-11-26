package com.escapehub.game.domain;

import java.util.List;

import com.escapehub.mission.domain.Evidence;
import com.escapehub.mission.domain.Mission;

/**
 * Ποια στοιχεία μιας αποστολής μπορεί να εξετάσει η ομάδα, ώστε να μπουν στο ημερολόγιο.
 *
 * <p>Sprint 1 (#28): μόνο όσα ο ορισμός της αποστολής δίνει από την αρχή ({@code initial: true}). Οι κανόνες
 * ξεκλειδώματος, όπου ένας λυμένος γρίφος αποκαλύπτει νέα στοιχεία, έρχονται με το US-10. Ο έλεγχος γίνεται πάντα
 * στο backend, ποτέ μόνο στο UI.
 */
public final class EvidenceAvailability {

	private EvidenceAvailability() {
	}

	/**
	 * Τα διαθέσιμα στοιχεία, με τη σειρά του αρχείου.
	 */
	public static List<Evidence> available(Mission mission) {
		return mission.initialEvidence();
	}

	/**
	 * Είναι το στοιχείο διαθέσιμο; Ένα id που δεν υπάρχει στην αποστολή δεν είναι διαθέσιμο.
	 */
	public static boolean isAvailable(Mission mission, String evidenceId) {
		return available(mission).stream().anyMatch(evidence -> evidence.id().equals(evidenceId));
	}

}
