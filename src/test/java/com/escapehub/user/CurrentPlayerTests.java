package com.escapehub.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapehub.support.TestPlayerConfiguration;

@WebMvcTest(CurrentPlayerTests.MeController.class)
@Import({ TestPlayerConfiguration.class, CurrentPlayerTests.MeController.class })
class CurrentPlayerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void identifiedPlayerIsPassedToController() {
		assertThat(mvc.get().uri("/test/me").header(TestPlayerConfiguration.HEADER, "7"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.player")
			.isEqualTo(7);
	}

	@Test
	void missingPlayerIs401ProblemDetail() {
		assertThat(mvc.get().uri("/test/me"))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.title")
			.isEqualTo("Δεν έχει οριστεί παίκτης");
	}

	@RestController
	static class MeController {

		@GetMapping("/test/me")
		Map<String, Long> me(@CurrentPlayer PlayerId player) {
			return Map.of("player", player.value());
		}

	}

}
