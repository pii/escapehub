package com.escapehub.game.application;

import java.time.Instant;
import java.util.List;

import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.team.TeamId;

/**
 * Ένα playthrough όπως το βλέπουν τα μέλη της ομάδας του (US-06). Δεν περιέχει γρίφους ούτε απαντήσεις.
 *
 * @param id το αναγνωριστικό
 * @param team η ομάδα
 * @param teamName το όνομα της ομάδας
 * @param missionId η αποστολή
 * @param missionVersion η έκδοση της αποστολής στην έναρξη
 * @param missionTitle ο τίτλος της αποστολής
 * @param paths οι διαδρομές της αποστολής
 * @param startedAt πότε ξεκίνησε
 * @param completedAt πότε ολοκληρώθηκε, ή {@code null}
 * @param status σε εξέλιξη ή ολοκληρωμένο
 */
public record PlaythroughDetails(PlaythroughId id, TeamId team, String teamName, String missionId,
		int missionVersion, String missionTitle, List<MissionPath> paths, Instant startedAt,
		Instant completedAt, MissionStatus status) {

	public PlaythroughDetails {
		paths = List.copyOf(paths);
	}

}
