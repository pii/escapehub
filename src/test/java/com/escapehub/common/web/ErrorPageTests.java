package com.escapehub.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.escapehub.support.TestcontainersConfiguration;

/**
 * Οι σελίδες σφάλματος του UI, με πραγματικό server (το MockMvc δεν περνά από τη σελίδα {@code /error}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ErrorPageTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	void unknownPageShowsHtml404() throws IOException, InterruptedException {
		HttpResponse<String> response = get("/no-such-page", "text/html");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("text/html"));
		assertThat(response.body()).contains("Η σελίδα δεν βρέθηκε");
	}

	@Test
	void apiWithoutPlayerIsJsonNotHtml() throws IOException, InterruptedException {
		HttpResponse<String> response = get("/api/players/me", "application/json");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	private HttpResponse<String> get(String path, String accept) throws IOException, InterruptedException {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Accept", accept)
				.GET()
				.build();
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		}
	}

}
