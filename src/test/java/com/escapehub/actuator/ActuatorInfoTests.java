package com.escapehub.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorInfoTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void infoShowsBuildVersion() {
		assertThat(mvc.get().uri("/actuator/info"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.build.version")
			.isNotNull();
	}

	@Test
	void infoShowsGitCommit() {
		assertThat(mvc.get().uri("/actuator/info"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.git.commit.id")
			.isNotNull();
	}

}
