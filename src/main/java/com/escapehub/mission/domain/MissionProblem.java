package com.escapehub.mission.domain;

import java.io.Serializable;

/**
 * Ένα πρόβλημα σε αρχείο αποστολής.
 *
 * @param field το πεδίο, π.χ. {@code puzzles[2].unlockedBy[0]}. Οι δείκτες ξεκινούν από το 0.
 * @param message τι είναι λάθος
 */
public record MissionProblem(String field, String message) implements Serializable {

	@Override
	public String toString() {
		return field.isEmpty() ? message : field + ": " + message;
	}

}
