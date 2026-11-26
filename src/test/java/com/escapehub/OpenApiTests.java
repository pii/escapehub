package com.escapehub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.support.IntegrationTest;

/**
 * Η τεκμηρίωση του REST API με OpenAPI (#26, TS-07, AC6).
 */
@IntegrationTest
class OpenApiTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void apiDocsDescribeOnlyTheRestApi() {
		assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk()
			.bodyJson()
			.hasPath("$.paths['/api/players']")
			.hasPath("$.paths['/api/teams']")
			.hasPath("$.paths['/api/missions']")
			.hasPath("$.paths['/api/playthroughs']")
			.hasPath("$.paths['/api/playthroughs/{id}']")
			.hasPath("$.paths['/api/playthroughs/{id}/journal']")
			.doesNotHavePath("$.paths['/teams']")
			.doesNotHavePath("$.paths['/missions']")
			.doesNotHavePath("$.paths['/player']");
		assertThat(mvc.get().uri("/v3/api-docs")).bodyJson()
			.extractingPath("$.info.title")
			.isEqualTo("EscapeHub API");
	}

	@Test
	void currentPlayerIsNotDocumentedAsRequestParameter() {
		assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk()
			.bodyJson()
			.hasPath("$.paths['/api/teams'].get")
			.doesNotHavePath("$.paths['/api/teams'].get.parameters");
	}

	@Test
	void swaggerUiIsAvailableWithoutPlayerName() {
		assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus3xxRedirection()
			.redirectedUrl()
			.contains("/swagger-ui/index.html");
		assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
	}

}
