package com.escapehub.user.web;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.escapehub.user.CurrentPlayerProvider;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Αναγνωρίζει τον παίκτη από το cookie {@link PlayerCookie#NAME}. Το αποτέλεσμα κρατιέται για όλο το αίτημα,
 * ώστε η βάση να ρωτιέται μία φορά.
 */
@Component
class CookieCurrentPlayerProvider implements CurrentPlayerProvider {

	private static final String CACHE_ATTRIBUTE = CookieCurrentPlayerProvider.class.getName() + ".player";

	private final PlayerService players;

	CookieCurrentPlayerProvider(PlayerService players) {
		this.players = players;
	}

	@Override
	public Optional<PlayerId> currentPlayer(HttpServletRequest request) {
		if (request.getAttribute(CACHE_ATTRIBUTE) instanceof PlayerId cached) {
			return Optional.of(cached);
		}
		Optional<PlayerId> player = PlayerCookie.read(request).flatMap(players::findByToken);
		player.ifPresent(id -> request.setAttribute(CACHE_ATTRIBUTE, id));
		return player;
	}

}
