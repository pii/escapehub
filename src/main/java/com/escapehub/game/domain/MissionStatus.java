package com.escapehub.game.domain;

import java.util.Collection;

/**
 * Η κατάσταση μιας αποστολής για μια ομάδα, όπως φαίνεται στον κατάλογο (US-05, AC2).
 */
public enum MissionStatus {

	NOT_STARTED("Δεν έχει ξεκινήσει"),

	IN_PROGRESS("Σε εξέλιξη"),

	COMPLETED("Ολοκληρώθηκε");

	private final String label;

	MissionStatus(String label) {
		this.label = label;
	}

	/**
	 * Το όνομα της κατάστασης όπως εμφανίζεται στο UI.
	 */
	public String label() {
		return label;
	}

	/**
	 * Η κατάσταση από τα playthroughs της ομάδας σε μία αποστολή. Ένα ενεργό playthrough σημαίνει «σε εξέλιξη»,
	 * ακόμη κι αν η ομάδα την έχει ολοκληρώσει παλιότερα.
	 */
	public static MissionStatus of(Collection<Playthrough> playthroughsOfMission) {
		if (playthroughsOfMission.stream().anyMatch(Playthrough::isActive)) {
			return IN_PROGRESS;
		}
		return playthroughsOfMission.isEmpty() ? NOT_STARTED : COMPLETED;
	}

	/**
	 * Η κατάσταση ενός playthrough.
	 */
	public static MissionStatus of(Playthrough playthrough) {
		return playthrough.isActive() ? IN_PROGRESS : COMPLETED;
	}

}
