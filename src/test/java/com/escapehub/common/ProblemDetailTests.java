package com.escapehub.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ProblemDetailTests.ProblemController.class)
@Import(ProblemDetailTests.ProblemController.class)
class ProblemDetailTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void notFoundIs404ProblemDetail() {
		assertThat(mvc.get().uri("/test/problems/not-found"))
			.hasStatus(HttpStatus.NOT_FOUND)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Η αποστολή δεν υπάρχει.");
	}

	@Test
	void conflictIs409ProblemDetail() {
		assertThat(mvc.get().uri("/test/problems/conflict"))
			.hasStatus(HttpStatus.CONFLICT)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo(409);
	}

	@Test
	void invalidBodyIs400WithFieldErrors() {
		assertThat(mvc.post().uri("/test/problems/validated")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"\"}"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.errors.name")
			.isNotNull();
	}

	@RestController
	static class ProblemController {

		@GetMapping("/test/problems/not-found")
		String notFound() {
			throw new NotFoundException("Η αποστολή δεν υπάρχει.");
		}

		@GetMapping("/test/problems/conflict")
		String conflict() {
			throw new ConflictException("Ο γρίφος είναι κλειδωμένος.");
		}

		@PostMapping("/test/problems/validated")
		String validated(@Valid @RequestBody NameRequest request) {
			return request.name();
		}

	}

	record NameRequest(@NotBlank String name) {
	}

}
