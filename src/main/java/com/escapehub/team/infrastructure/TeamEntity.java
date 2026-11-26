package com.escapehub.team.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Γραμμή του πίνακα {@code team}. Ο δημιουργός αποθηκεύεται ως id παίκτη, χωρίς σχέση JPA με το module
 * {@code user}.
 */
@Entity
@Table(name = "team")
public class TeamEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 40)
	private String name;

	@Column(name = "invite_code", nullable = false, length = 8)
	private String inviteCode;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected TeamEntity() {
	}

	public TeamEntity(String name, String inviteCode, long creatorId) {
		this.name = name;
		this.inviteCode = inviteCode;
		this.creatorId = creatorId;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getInviteCode() {
		return inviteCode;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
