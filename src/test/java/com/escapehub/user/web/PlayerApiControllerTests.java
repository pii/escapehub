package com.escapehub.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.ConflictException;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;
import com.escapehub.user.application.RegisteredPlayer;

@WebMvcTest(PlayerApiController.class)
@Import(TestPlayerConfiguration.class)
class PlayerApiControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlayerService players;

	@Test
	void registrationReturns201WithCookie() {
		when(players.register("Maria")).thenReturn(new RegisteredPlayer(new PlayerId(1), "Maria", "token-1"));

		assertThat(post("{\"name\": \"Maria\"}"))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "/api/players/me")
			.cookies()
			.hasValue(PlayerCookie.NAME, "token-1")
			.isHttpOnly(PlayerCookie.NAME, true);
	}

	@Test
	void registrationReturnsPlayer() {
		when(players.register("Maria")).thenReturn(new RegisteredPlayer(new PlayerId(1), "Maria", "token-1"));

		assertThat(post("{\"name\": \"Maria\"}"))
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("Maria");
	}

	@Test
	void takenNameIs409ProblemDetail() {
		when(players.register("Maria")).thenThrow(new ConflictException("Το όνομα χρησιμοποιείται ήδη."));

		assertThat(post("{\"name\": \"Maria\"}"))
			.hasStatus(HttpStatus.CONFLICT)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Το όνομα χρησιμοποιείται ήδη.");
	}

	@Test
	void invalidNameIs400ProblemDetail() {
		when(players.register("ab"))
			.thenThrow(new BadRequestException("Το όνομα πρέπει να έχει 3–20 χαρακτήρες."));

		assertThat(post("{\"name\": \"ab\"}"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void missingNameIs400WithFieldError() {
		assertThat(post("{}"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors.name")
			.isNotNull();
	}

	@Test
	void meWithoutPlayerIs401() {
		assertThat(mvc.get().uri("/api/players/me"))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void meReturnsCurrentPlayer() {
		when(players.findName(new PlayerId(7))).thenReturn(Optional.of("Maria"));

		assertThat(mvc.get().uri("/api/players/me").header(TestPlayerConfiguration.HEADER, "7"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("Maria");
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post().uri("/api/players").contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
