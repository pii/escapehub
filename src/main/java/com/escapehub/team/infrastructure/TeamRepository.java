package com.escapehub.team.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Πρόσβαση στον πίνακα {@code team}. Τα άλλα modules χρησιμοποιούν το {@code TeamService}, όχι αυτό.
 */
public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

	boolean existsByInviteCode(String inviteCode);

	Optional<TeamEntity> findByInviteCode(String inviteCode);

}
