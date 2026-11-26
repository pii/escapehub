package com.escapehub.game.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.escapehub.common.ConflictException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.EvidenceAvailability;
import com.escapehub.journal.application.JournalEntry;
import com.escapehub.journal.application.JournalService;
import com.escapehub.mission.application.MissionCatalog;
import com.escapehub.mission.domain.Evidence;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Τα αντικείμενα που εξετάζει η ομάδα και το κοινό ημερολόγιο ενός playthrough (US-07). Ελέγχει ότι ο παίκτης
 * είναι μέλος της ομάδας και ότι το στοιχείο είναι διαθέσιμο, πριν το γράψει στο ημερολόγιο του module
 * {@code journal}.
 */
@Service
public class EvidenceService {

	static final String NOT_AVAILABLE = "Το στοιχείο δεν έχει αποκαλυφθεί ακόμη.";

	static final String COMPLETED = "Η αποστολή έχει ολοκληρωθεί. Το ημερολόγιο δεν αλλάζει πια.";

	private final PlaythroughService playthroughs;

	private final MissionCatalog missions;

	private final JournalService journal;

	private final PlayerService players;

	public EvidenceService(PlaythroughService playthroughs, MissionCatalog missions, JournalService journal,
			PlayerService players) {
		this.playthroughs = playthroughs;
		this.missions = missions;
		this.journal = journal;
		this.players = players;
	}

	/**
	 * Τα διαθέσιμα αντικείμενα, με ένδειξη για όσα είναι ήδη στο ημερολόγιο.
	 *
	 * @throws NotFoundException αν το playthrough δεν υπάρχει
	 * @throws ForbiddenException αν ο παίκτης δεν είναι μέλος της ομάδας
	 */
	@Transactional(readOnly = true)
	public List<ExaminableEvidence> examinable(PlayerId player, PlaythroughId id) {
		Optional<Mission> mission = missions.find(playthroughs.details(player, id).missionId());
		if (mission.isEmpty()) {
			return List.of();
		}
		Map<String, String> pathTitles = mission.get()
			.paths()
			.stream()
			.collect(Collectors.toMap(MissionPath::id, MissionPath::title));
		Set<String> inJournal = journal.entries(id)
			.stream()
			.map(JournalEntry::evidenceId)
			.collect(Collectors.toSet());
		return EvidenceAvailability.available(mission.get())
			.stream()
			.map(item -> new ExaminableEvidence(item.id(), item.title(),
					pathTitles.getOrDefault(item.path(), ""), inJournal.contains(item.id())))
			.toList();
	}

	/**
	 * Το ημερολόγιο, με τα νεότερα πρώτα. Το βλέπουν όλα τα μέλη της ομάδας.
	 *
	 * @throws NotFoundException αν το playthrough δεν υπάρχει
	 * @throws ForbiddenException αν ο παίκτης δεν είναι μέλος της ομάδας
	 */
	@Transactional(readOnly = true)
	public List<JournalItem> journal(PlayerId player, PlaythroughId id) {
		Optional<Mission> mission = missions.find(playthroughs.details(player, id).missionId());
		Map<PlayerId, String> names = new HashMap<>();
		return journal.entries(id).stream().map(entry -> toItem(entry, mission, names)).toList();
	}

	/**
	 * Εξέταση αντικειμένου: το στοιχείο του μπαίνει στο ημερολόγιο με τον παίκτη και την ώρα. Αν είναι ήδη εκεί,
	 * δεν προστίθεται ξανά και δεν είναι σφάλμα.
	 *
	 * @throws NotFoundException αν το playthrough δεν υπάρχει
	 * @throws ForbiddenException αν ο παίκτης δεν είναι μέλος της ομάδας
	 * @throws ConflictException αν το στοιχείο δεν είναι διαθέσιμο (ή δεν υπάρχει), ή αν η αποστολή ολοκληρώθηκε
	 */
	@Transactional
	public ExamineResult examine(PlayerId player, PlaythroughId id, String evidenceId) {
		PlaythroughDetails playthrough = playthroughs.details(player, id);
		if (playthrough.completedAt() != null) {
			throw new ConflictException(COMPLETED);
		}
		Optional<Mission> mission = missions.find(playthrough.missionId());
		if (mission.isEmpty() || !EvidenceAvailability.isAvailable(mission.get(), evidenceId)) {
			throw new ConflictException(NOT_AVAILABLE);
		}
		boolean added = journal.record(id, evidenceId, player);
		JournalEntry entry = journal.entry(id, evidenceId)
			.orElseThrow(() -> new IllegalStateException("Λείπει η καταχώριση μετά την προσθήκη"));
		return new ExamineResult(toItem(entry, mission, new HashMap<>()), added);
	}

	private JournalItem toItem(JournalEntry entry, Optional<Mission> mission, Map<PlayerId, String> names) {
		String title = mission.flatMap(found -> found.findEvidence(entry.evidenceId()))
			.map(Evidence::title)
			.orElse(entry.evidenceId());
		String name = names.computeIfAbsent(entry.addedBy(),
				player -> players.findName(player).orElse("Άγνωστος παίκτης"));
		return new JournalItem(entry.evidenceId(), title, entry.addedBy(), name, entry.addedAt());
	}

}
