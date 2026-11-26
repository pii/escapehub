package com.escapehub.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.support.TestPlayerConfiguration;

@WebMvcTest(HomeController.class)
@Import(TestPlayerConfiguration.class)
class HomeControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void pageWithoutPlayerRedirectsToNamePage() {
		assertThat(mvc.get().uri("/"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/player");
	}

	@Test
	void pageWithPlayerUsesLayout() {
		assertThat(mvc.get().uri("/").header(TestPlayerConfiguration.HEADER, "7"))
			.hasStatusOk()
			.hasViewName("index")
			.bodyText()
			.contains("EscapeHub")
			.contains("/css/escapehub.css");
	}

}
