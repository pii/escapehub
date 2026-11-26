package com.escapehub.user.web;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapehub.common.UnauthenticatedException;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;
import com.escapehub.user.application.RegisteredPlayer;

/**
 * REST API για τους παίκτες (US-01). Τα σφάλματα επιστρέφονται ως {@code ProblemDetail}.
 */
@RestController
@RequestMapping("/api/players")
class PlayerApiController {

	private final PlayerService players;

	PlayerApiController(PlayerService players) {
		this.players = players;
	}

	/**
	 * Δημιουργεί παίκτη και στέλνει το cookie που τον αναγνωρίζει. Απαντά 201, ή 400 για άκυρο όνομα, ή 409 αν
	 * το όνομα χρησιμοποιείται ήδη.
	 */
	@PostMapping
	ResponseEntity<PlayerResponse> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
		RegisteredPlayer player = players.register(body.name());
		return ResponseEntity.created(URI.create("/api/players/me"))
			.header(HttpHeaders.SET_COOKIE, PlayerCookie.create(player.token(), request))
			.body(new PlayerResponse(player.id().value(), player.name()));
	}

	/**
	 * Ο τρέχων παίκτης. Απαντά 401 αν δεν υπάρχει αναγνωρισμένος παίκτης.
	 */
	@GetMapping("/me")
	PlayerResponse me(@CurrentPlayer PlayerId player) {
		String name = players.findName(player)
			.orElseThrow(() -> new UnauthenticatedException("Ο παίκτης δεν υπάρχει πια."));
		return new PlayerResponse(player.value(), name);
	}

	record RegisterRequest(@NotNull String name) {
	}

	record PlayerResponse(long id, String name) {
	}

}
