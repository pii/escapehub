package com.escapehub.journal.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.escapehub.game.PlaythroughId;
import com.escapehub.journal.infrastructure.JournalEntryEntity;
import com.escapehub.journal.infrastructure.JournalEntryRepository;
import com.escapehub.user.PlayerId;

/**
 * Το κοινό ημερολόγιο ενός playthrough (US-07). Είναι το public API του module {@code journal}.
 *
 * <p>Δεν ελέγχει δικαιώματα ούτε αν ένα στοιχείο είναι διαθέσιμο: αυτά τα ελέγχει το module {@code game} πριν
 * καλέσει το {@link #record}, γιατί εκεί είναι οι κανόνες της αποστολής.
 */
@Service
public class JournalService {

	private final JournalEntryRepository entries;

	public JournalService(JournalEntryRepository entries) {
		this.entries = entries;
	}

	/**
	 * Προσθέτει το στοιχείο, αν δεν υπάρχει ήδη. Είναι idempotent, και σε ταυτόχρονα αιτήματα.
	 *
	 * @return {@code true} αν προστέθηκε τώρα, {@code false} αν υπήρχε ήδη
	 */
	@Transactional
	public boolean record(PlaythroughId playthrough, String evidenceId, PlayerId player) {
		return entries.saveIfAbsent(playthrough.value(), evidenceId, player.value()) == 1;
	}

	/**
	 * Η καταχώριση ενός στοιχείου, αν υπάρχει.
	 */
	@Transactional(readOnly = true)
	public Optional<JournalEntry> entry(PlaythroughId playthrough, String evidenceId) {
		return entries.findByPlaythroughIdAndEvidenceId(playthrough.value(), evidenceId)
			.map(JournalService::toEntry);
	}

	/**
	 * Όλες οι καταχωρίσεις, οι νεότερες πρώτες.
	 */
	@Transactional(readOnly = true)
	public List<JournalEntry> entries(PlaythroughId playthrough) {
		return entries.findByPlaythroughIdOrderByAddedAtDescIdDesc(playthrough.value())
			.stream()
			.map(JournalService::toEntry)
			.toList();
	}

	private static JournalEntry toEntry(JournalEntryEntity entity) {
		return new JournalEntry(entity.getEvidenceId(), new PlayerId(entity.getAddedBy()), entity.getAddedAt());
	}

}
