package com.escapehub.user.application;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.escapehub.common.ConflictException;
import com.escapehub.user.PlayerId;
import com.escapehub.user.domain.PlayerName;
import com.escapehub.user.infrastructure.PlayerEntity;
import com.escapehub.user.infrastructure.PlayerRepository;

/**
 * Εγγραφή και αναζήτηση παικτών. Είναι το public API του module {@code user} για τα άλλα modules.
 */
@Service
public class PlayerService {

	static final String NAME_TAKEN = "Το όνομα χρησιμοποιείται ήδη.";

	private final PlayerRepository players;

	public PlayerService(PlayerRepository players) {
		this.players = players;
	}

	/**
	 * Δημιουργεί παίκτη με μοναδικό όνομα. Η σύγκριση αγνοεί κεφαλαία-πεζά και κενά στην αρχή και στο τέλος.
	 *
	 * @throws com.escapehub.common.BadRequestException αν το όνομα δεν είναι έγκυρο
	 * @throws ConflictException αν το όνομα χρησιμοποιείται ήδη, και όταν δύο αιτήματα το ζητούν ταυτόχρονα
	 */
	@Transactional
	public RegisteredPlayer register(String rawName) {
		PlayerName name = PlayerName.of(rawName);
		if (players.existsByUsernameIgnoreCase(name.value())) {
			throw new ConflictException(NAME_TAKEN);
		}
		String token = PlayerTokens.newToken();
		try {
			PlayerEntity entity = new PlayerEntity(name.value(), PlayerTokens.hash(token));
			PlayerEntity saved = players.saveAndFlush(entity);
			return new RegisteredPlayer(new PlayerId(saved.getId()), saved.getUsername(), token);
		}
		catch (DataIntegrityViolationException ex) {
			// Δύο ταυτόχρονες εγγραφές με το ίδιο όνομα:
			// το μοναδικό index στο lower(username) κρατά μόνο μία.
			throw new ConflictException(NAME_TAKEN);
		}
	}

	/**
	 * Ο παίκτης στον οποίο ανήκει το token του cookie.
	 */
	@Transactional(readOnly = true)
	public Optional<PlayerId> findByToken(String token) {
		return players.findByTokenHash(PlayerTokens.hash(token))
			.map(player -> new PlayerId(player.getId()));
	}

	/**
	 * Το όνομα ενός παίκτη, π.χ. για εμφάνιση στο UI.
	 */
	@Transactional(readOnly = true)
	public Optional<String> findName(PlayerId player) {
		return players.findById(player.value()).map(PlayerEntity::getUsername);
	}

}
