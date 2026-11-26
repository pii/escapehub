package com.escapehub.game.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Γραμμή του πίνακα {@code playthrough}. Δημιουργείται μόνο με {@link PlaythroughRepository#startIfAbsent}, ώστε
 * να μην υπάρχουν δύο ενεργά playthroughs της ίδιας αποστολής για την ίδια ομάδα.
 */
@Entity
@Table(name = "playthrough")
public class PlaythroughEntity {

	@Id
	private Long id;

	@Column(name = "team_id", nullable = false)
	private Long teamId;

	@Column(name = "mission_id", nullable = false, length = 100)
	private String missionId;

	@Column(name = "mission_version", nullable = false)
	private int missionVersion;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Version
	private long version;

	protected PlaythroughEntity() {
	}

	public Long getId() {
		return id;
	}

	public Long getTeamId() {
		return teamId;
	}

	public String getMissionId() {
		return missionId;
	}

	public int getMissionVersion() {
		return missionVersion;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

}
