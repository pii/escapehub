package com.escapehub.game.application;

import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.mission.domain.Mission;

/**
 * Μια αποστολή στον κατάλογο (US-05), με την κατάσταση για την επιλεγμένη ομάδα. Δεν περιέχει γρίφους ούτε
 * απαντήσεις.
 *
 * @param missionId το id της αποστολής
 * @param title ο τίτλος
 * @param summary η σύντομη περιγραφή
 * @param estimatedMinutes η εκτιμώμενη διάρκεια σε λεπτά
 * @param minPlayers ο ελάχιστος αριθμός παικτών
 * @param status η κατάσταση για την ομάδα, ή {@code null} αν δεν επιλέχθηκε ομάδα
 * @param activePlaythrough το ενεργό playthrough της ομάδας, ή {@code null} αν δεν υπάρχει
 */
public record CatalogEntry(String missionId, String title, String summary, int estimatedMinutes, int minPlayers,
		MissionStatus status, PlaythroughId activePlaythrough) {

	static CatalogEntry of(Mission mission, MissionStatus status, PlaythroughId activePlaythrough) {
		return new CatalogEntry(mission.id(), mission.title(), mission.summary(), mission.estimatedMinutes(),
				mission.minPlayers(), status, activePlaythrough);
	}

}
