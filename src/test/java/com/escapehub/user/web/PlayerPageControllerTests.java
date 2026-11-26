package com.escapehub.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.ConflictException;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;
import com.escapehub.user.application.RegisteredPlayer;

@WebMvcTest(PlayerPageController.class)
class PlayerPageControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlayerService players;

	@Test
	void showsNameForm() {
		assertThat(mvc.get().uri("/player"))
			.hasStatusOk()
			.hasViewName("player/name")
			.bodyText()
			.contains("Ποιο είναι το όνομά σου;");
	}

	@Test
	void registrationSetsCookieAndRedirects() {
		when(players.register("Μαρία")).thenReturn(new RegisteredPlayer(new PlayerId(1), "Μαρία", "token-1"));

		assertThat(mvc.post().uri("/player").param("name", "Μαρία"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/")
			.cookies()
			.hasValue(PlayerCookie.NAME, "token-1")
			.isHttpOnly(PlayerCookie.NAME, true);
	}

	@Test
	void takenNameShowsFormAgainWithMessage() {
		when(players.register("Maria")).thenThrow(new ConflictException("Το όνομα χρησιμοποιείται ήδη."));

		assertThat(mvc.post().uri("/player").param("name", "Maria"))
			.hasStatusOk()
			.hasViewName("player/name")
			.bodyText()
			.contains("Το όνομα χρησιμοποιείται ήδη.");
	}

	@Test
	void invalidNameShowsFormAgainWithMessage() {
		when(players.register("ab"))
			.thenThrow(new BadRequestException("Το όνομα πρέπει να έχει 3–20 χαρακτήρες."));

		assertThat(mvc.post().uri("/player").param("name", "ab"))
			.hasStatusOk()
			.hasViewName("player/name")
			.bodyText()
			.contains("3–20 χαρακτήρες");
	}

}
