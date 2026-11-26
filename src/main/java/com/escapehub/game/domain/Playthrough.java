package com.escapehub.game.domain;

import java.time.Instant;

import com.escapehub.game.PlaythroughId;
import com.escapehub.team.TeamId;

/**
 * Ένα παιχνίδι μιας ομάδας σε μια αποστολή.
 *
 * @param id το αναγνωριστικό
 * @param team η ομάδα που παίζει
 * @param missionId η αποστολή
 * @param missionVersion η έκδοση του αρχείου της αποστολής στην έναρξη
 * @param startedAt πότε ξεκίνησε
 * @param completedAt πότε ολοκληρώθηκε, ή {@code null} αν είναι ενεργό
 */
public record Playthrough(PlaythroughId id, TeamId team, String missionId, int missionVersion, Instant startedAt,
		Instant completedAt) {

	public boolean isActive() {
		return completedAt == null;
	}

}
