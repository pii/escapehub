package com.escapehub.user.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Γραμμή του πίνακα {@code player}. Ο πίνακας δημιουργείται από τη migration {@code create_player}.
 */
@Entity
@Table(name = "player")
public class PlayerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String username;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PlayerEntity() {
	}

	public PlayerEntity(String username, String tokenHash) {
		this.username = username;
		this.tokenHash = tokenHash;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
