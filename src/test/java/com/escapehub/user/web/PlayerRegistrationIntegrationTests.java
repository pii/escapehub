package com.escapehub.user.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.escapehub.common.ConflictException;
import com.escapehub.support.IntegrationTest;
import com.escapehub.user.application.PlayerService;
import com.escapehub.user.application.RegisteredPlayer;
import com.escapehub.user.infrastructure.PlayerRepository;

/**
 * Η εγγραφή παίκτη από άκρη σε άκρη, με πραγματική PostgreSQL. Κάθε έλεγχος χρησιμοποιεί διαφορετικό όνομα,
 * γιατί η βάση είναι κοινή για όλους τους ελέγχους.
 */
@IntegrationTest
class PlayerRegistrationIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private PlayerService players;

	@Autowired
	private PlayerRepository repository;

	@Test
	void registeredPlayerIsRecognisedByCookie() {
		MvcTestResult registration = mvc.post().uri("/api/players")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"Ελένη\"}")
			.exchange();
		assertThat(registration).hasStatus(HttpStatus.CREATED);
		Cookie cookie = registration.getResponse().getCookie(PlayerCookie.NAME);
		assertThat(cookie).isNotNull();

		assertThat(mvc.get().uri("/api/players/me").cookie(cookie))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.name")
			.isEqualTo("Ελένη");
	}

	@Test
	void nameIsUniqueIgnoringCaseAndSurroundingSpaces() {
		players.register("Kostas");

		assertThat(mvc.post().uri("/api/players")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"  KOSTAS \"}"))
			.hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void concurrentRegistrationsOfSameNameCreateOnePlayer() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<RegisteredPlayer>> attempts = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
			for (int i = 0; i < 4; i++) {
				attempts.add(executor.submit(() -> {
					start.await();
					return players.register("Concurrent");
				}));
			}
			start.countDown();
		}

		int created = 0;
		int conflicts = 0;
		for (Future<RegisteredPlayer> attempt : attempts) {
			try {
				attempt.get();
				created++;
			}
			catch (java.util.concurrent.ExecutionException ex) {
				assertThat(ex.getCause()).isInstanceOf(ConflictException.class);
				conflicts++;
			}
		}
		assertThat(created).isEqualTo(1);
		assertThat(conflicts).isEqualTo(3);
	}

	@Test
	void onlyTheTokenHashIsStored() {
		RegisteredPlayer player = players.register("HashCheck");

		assertThat(repository.findById(player.id().value()))
			.hasValueSatisfying(entity -> assertThat(entity.getTokenHash())
				.hasSize(64)
				.isNotEqualTo(player.token()));
	}

	@Test
	void namePageFlowShowsPlayerInLayout() {
		MvcTestResult registration = mvc.post().uri("/player").param("name", "Πέτρος").exchange();
		assertThat(registration).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/");
		Cookie cookie = registration.getResponse().getCookie(PlayerCookie.NAME);
		assertThat(cookie).isNotNull();

		assertThat(mvc.get().uri("/").cookie(cookie))
			.hasStatusOk()
			.bodyText()
			.contains("Πέτρος");
	}

}
