package com.escapehub.journal.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Γραμμή του πίνακα {@code journal_entry}. Οι εγγραφές γίνονται μόνο με
 * {@link JournalEntryRepository#saveIfAbsent}, ώστε να μη δημιουργούνται διπλές.
 */
@Entity
@Table(name = "journal_entry")
public class JournalEntryEntity {

	@Id
	private Long id;

	@Column(name = "playthrough_id", nullable = false)
	private Long playthroughId;

	@Column(name = "evidence_id", nullable = false, length = 100)
	private String evidenceId;

	@Column(name = "added_by", nullable = false)
	private Long addedBy;

	@Column(name = "added_at", nullable = false)
	private Instant addedAt;

	protected JournalEntryEntity() {
	}

	public Long getId() {
		return id;
	}

	public Long getPlaythroughId() {
		return playthroughId;
	}

	public String getEvidenceId() {
		return evidenceId;
	}

	public Long getAddedBy() {
		return addedBy;
	}

	public Instant getAddedAt() {
		return addedAt;
	}

}
