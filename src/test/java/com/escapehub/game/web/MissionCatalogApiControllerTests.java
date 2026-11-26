package com.escapehub.game.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.ForbiddenException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.CatalogEntry;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamId;
import com.escapehub.user.PlayerId;

@WebMvcTest(MissionCatalogApiController.class)
@Import(TestPlayerConfiguration.class)
class MissionCatalogApiControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlaythroughService playthroughs;

	@Test
	void catalogListsTitleSummaryDurationAndMinPlayers() {
		when(playthroughs.catalog(MARIA, null)).thenReturn(List.of(entry(null, null)));

		var json = assertThat(get("/api/missions")).hasStatusOk().bodyJson();
		json.extractingPath("$[0].id").isEqualTo("villa-diodati");
		json.extractingPath("$[0].title").isEqualTo("Το μυστήριο της Villa Diodati");
		json.extractingPath("$[0].summary").isEqualTo("Ένα κλειδωμένο αρχείο.");
		json.extractingPath("$[0].estimatedMinutes").isEqualTo(45);
		json.extractingPath("$[0].minPlayers").isEqualTo(2);
		json.extractingPath("$[0].status").isNull();
	}

	@Test
	void catalogWithTeamShowsStatusAndActivePlaythrough() {
		when(playthroughs.catalog(MARIA, new TeamId(10)))
			.thenReturn(List.of(entry(MissionStatus.IN_PROGRESS, new PlaythroughId(5))));

		var json = assertThat(get("/api/missions?teamId=10")).hasStatusOk().bodyJson();
		json.extractingPath("$[0].status").isEqualTo("IN_PROGRESS");
		json.extractingPath("$[0].playthroughId").isEqualTo(5);
	}

	@Test
	void catalogOfOtherTeamIs403() {
		when(playthroughs.catalog(MARIA, new TeamId(99)))
			.thenThrow(new ForbiddenException("Δεν είσαι μέλος αυτής της ομάδας."));

		assertThat(get("/api/missions?teamId=99")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get("/api/missions?teamId=0")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void catalogWithoutPlayerIs401() {
		assertThat(mvc.get().uri("/api/missions")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MockMvcTester.MockMvcRequestBuilder get(String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, "1");
	}

	private static CatalogEntry entry(MissionStatus status, PlaythroughId active) {
		return new CatalogEntry("villa-diodati", "Το μυστήριο της Villa Diodati", "Ένα κλειδωμένο αρχείο.", 45,
				2, status, active);
	}

}
