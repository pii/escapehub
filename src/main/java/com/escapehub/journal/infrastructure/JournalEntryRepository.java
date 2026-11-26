package com.escapehub.journal.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Πρόσβαση στον πίνακα {@code journal_entry}.
 */
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Long> {

	/**
	 * Προσθέτει το στοιχείο στο ημερολόγιο, αν δεν υπάρχει ήδη. Η απόφαση γίνεται στη βάση
	 * ({@code on conflict do nothing}), οπότε ισχύει και για ταυτόχρονα αιτήματα, χωρίς σφάλμα για κανέναν.
	 *
	 * @return 1 αν προστέθηκε, 0 αν υπήρχε ήδη
	 */
	@Modifying
	@Query(value = """
			insert into journal_entry (playthrough_id, evidence_id, added_by, added_at)
			values (:playthroughId, :evidenceId, :addedBy, now())
			on conflict (playthrough_id, evidence_id) do nothing
			""", nativeQuery = true)
	int saveIfAbsent(@Param("playthroughId") long playthroughId, @Param("evidenceId") String evidenceId,
			@Param("addedBy") long addedBy);

	Optional<JournalEntryEntity> findByPlaythroughIdAndEvidenceId(Long playthroughId, String evidenceId);

	List<JournalEntryEntity> findByPlaythroughIdOrderByAddedAtDescIdDesc(Long playthroughId);

}
