package com.escapehub.game.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.PlaythroughDetails;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.application.StartResult;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamId;
import com.escapehub.user.PlayerId;

@WebMvcTest(PlaythroughApiController.class)
@Import(TestPlayerConfiguration.class)
class PlaythroughApiControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final PlaythroughDetails PLAYTHROUGH = new PlaythroughDetails(new PlaythroughId(5),
			new TeamId(10), "Οι Ερευνητές", "villa-diodati", 1, "Το μυστήριο της Villa Diodati",
			List.of(new MissionPath("letter", "Η επιστολή")), Instant.parse("2026-11-26T14:00:00Z"), null,
			MissionStatus.IN_PROGRESS);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlaythroughService playthroughs;

	@Test
	void startReturns201WithLocationAndStartTime() {
		when(playthroughs.start(MARIA, new TeamId(10), "villa-diodati"))
			.thenReturn(new StartResult(PLAYTHROUGH, true));

		var json = assertThat(post("{\"missionId\": \"villa-diodati\", \"teamId\": 10}"))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "/api/playthroughs/5")
			.bodyJson();
		json.extractingPath("$.startedAt").isEqualTo("2026-11-26T14:00:00Z");
		json.extractingPath("$.missionVersion").isEqualTo(1);
		json.extractingPath("$.status").isEqualTo("IN_PROGRESS");
	}

	@Test
	void startWithActivePlaythroughReturns200WithIt() {
		when(playthroughs.start(MARIA, new TeamId(10), "villa-diodati"))
			.thenReturn(new StartResult(PLAYTHROUGH, false));

		assertThat(post("{\"missionId\": \"villa-diodati\", \"teamId\": 10}")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.id")
			.isEqualTo(5);
	}

	@Test
	void startByMemberIs403ProblemDetail() {
		String onlyCreator = "Μόνο ο δημιουργός της ομάδας μπορεί να ξεκινήσει αποστολή.";
		when(playthroughs.start(MARIA, new TeamId(10), "villa-diodati"))
			.thenThrow(new ForbiddenException(onlyCreator));

		assertThat(post("{\"missionId\": \"villa-diodati\", \"teamId\": 10}")).hasStatus(HttpStatus.FORBIDDEN)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo(onlyCreator);
	}

	@Test
	void soloStartUsesTeamOfOne() {
		when(playthroughs.startSolo(MARIA, "villa-diodati")).thenReturn(new StartResult(PLAYTHROUGH, true));

		assertThat(post("{\"missionId\": \"villa-diodati\", \"solo\": true}")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void invalidStartRequestsAre400() {
		assertThat(post("{\"missionId\": \"villa-diodati\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("{\"missionId\": \"villa-diodati\", \"teamId\": 10, \"solo\": true}"))
			.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("{\"missionId\": \"\", \"teamId\": 10}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void unknownMissionIs404() {
		when(playthroughs.start(MARIA, new TeamId(10), "nope"))
			.thenThrow(new NotFoundException("Δεν βρέθηκε."));

		assertThat(post("{\"missionId\": \"nope\", \"teamId\": 10}")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void memberSeesPlaythroughWithPaths() {
		when(playthroughs.details(MARIA, new PlaythroughId(5))).thenReturn(PLAYTHROUGH);

		var json = assertThat(get("/api/playthroughs/5")).hasStatusOk().bodyJson();
		json.extractingPath("$.missionTitle").isEqualTo("Το μυστήριο της Villa Diodati");
		json.extractingPath("$.teamName").isEqualTo("Οι Ερευνητές");
		json.extractingPath("$.paths[0].title").isEqualTo("Η επιστολή");
	}

	@Test
	void playthroughOfOtherTeamIs403() {
		when(playthroughs.details(MARIA, new PlaythroughId(5)))
			.thenThrow(new ForbiddenException("Το παιχνίδι ανήκει σε άλλη ομάδα."));

		assertThat(get("/api/playthroughs/5")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void unknownPlaythroughIs404() {
		when(playthroughs.details(MARIA, new PlaythroughId(99)))
			.thenThrow(new NotFoundException("Δεν βρέθηκε."));

		assertThat(get("/api/playthroughs/99")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/playthroughs/0")).hasStatus(HttpStatus.NOT_FOUND);
	}

	private MockMvcTester.MockMvcRequestBuilder get(String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, "1");
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post()
			.uri("/api/playthroughs")
			.header(TestPlayerConfiguration.HEADER, "1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json);
	}

}
