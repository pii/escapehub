package com.escapehub.team.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.escapehub.team.TeamRole;

/**
 * Γραμμή του πίνακα {@code team_membership}. Οι εγγραφές γίνονται μόνο με
 * {@link TeamMembershipRepository#saveIfAbsent}, ώστε να μη δημιουργούνται διπλές.
 */
@Entity
@Table(name = "team_membership")
public class TeamMembershipEntity {

	@Id
	private Long id;

	@Column(name = "team_id", nullable = false)
	private Long teamId;

	@Column(name = "player_id", nullable = false)
	private Long playerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TeamRole role;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	protected TeamMembershipEntity() {
	}

	public Long getId() {
		return id;
	}

	public Long getTeamId() {
		return teamId;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public TeamRole getRole() {
		return role;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

}
