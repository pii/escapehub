package com.escapehub.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(HelloController.class)
class HelloControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void helloUsesDefaultName() {
		assertThat(mvc.get().uri("/api/hello"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.message")
			.isEqualTo("Hello, world!");
	}

	@Test
	void helloUsesGivenName() {
		assertThat(mvc.get().uri("/api/hello").param("name", "EscapeHub"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.message")
			.isEqualTo("Hello, EscapeHub!");
	}

}
