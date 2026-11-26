package com.escapehub.user.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Πρόσβαση στον πίνακα {@code player}. Τα άλλα modules δεν τον χρησιμοποιούν απευθείας, αλλά μέσω του
 * {@code PlayerService}.
 */
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

	boolean existsByUsernameIgnoreCase(String username);

	Optional<PlayerEntity> findByTokenHash(String tokenHash);

}
