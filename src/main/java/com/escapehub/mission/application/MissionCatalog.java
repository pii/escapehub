package com.escapehub.mission.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.escapehub.common.NotFoundException;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.infrastructure.InvalidMissionException;
import com.escapehub.mission.infrastructure.MissionFiles;

/**
 * Οι αποστολές της εφαρμογής (TS-06). Είναι το public API του module {@code mission} για τα άλλα modules, π.χ.
 * για τον κατάλογο και την έναρξη αποστολής (#27) και για το ημερολόγιο (#28).
 *
 * <p>Τα αρχεία διαβάζονται και επικυρώνονται μία φορά, στην εκκίνηση. Ένα άκυρο αρχείο σταματά την εκκίνηση με
 * {@link InvalidMissionException}.
 *
 * <p>Τα {@link Mission} περιέχουν και τις σωστές απαντήσεις ({@code Puzzle.answer()}), που τις χρειάζεται ο
 * μηχανισμός του παιχνιδιού. Οι απαντήσεις δεν γράφονται σε JSON, αλλά ένα template ή ένα DTO δεν πρέπει να τις
 * διαβάζει ποτέ.
 */
@Service
public class MissionCatalog {

	static final String MISSION_NOT_FOUND = "Η αποστολή δεν βρέθηκε.";

	private final Map<String, Mission> missions = new LinkedHashMap<>();

	public MissionCatalog(MissionFiles files) {
		files.loadAll().forEach(mission -> missions.put(mission.id(), mission));
	}

	/**
	 * Όλες οι αποστολές, με τη σειρά των ονομάτων των αρχείων.
	 */
	public List<Mission> all() {
		return List.copyOf(missions.values());
	}

	public Optional<Mission> find(String id) {
		return Optional.ofNullable(missions.get(id));
	}

	/**
	 * Η αποστολή με αυτό το id.
	 *
	 * @throws NotFoundException αν δεν υπάρχει
	 */
	public Mission get(String id) {
		return find(id).orElseThrow(() -> new NotFoundException(MISSION_NOT_FOUND));
	}

}
