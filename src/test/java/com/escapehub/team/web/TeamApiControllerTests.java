package com.escapehub.team.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.NotFoundException;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.JoinResult;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamMember;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.PlayerId;

@WebMvcTest(TeamApiController.class)
@Import(TestPlayerConfiguration.class)
class TeamApiControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final TeamDetails TEAM = new TeamDetails(new TeamId(10), "Οι Ερευνητές", "ABCD2345",
			List.of(new TeamMember(MARIA, "Μαρία", TeamRole.CREATOR)));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TeamService teams;

	@Test
	void createReturns201WithLocationAndCode() {
		when(teams.create(MARIA, "Οι Ερευνητές")).thenReturn(TEAM);

		assertThat(post("/api/teams", "{\"name\": \"Οι Ερευνητές\"}"))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "/api/teams/10")
			.bodyJson()
			.extractingPath("$.inviteCode")
			.isEqualTo("ABCD2345");
	}

	@Test
	void blankNameIs400ProblemDetail() {
		when(teams.create(MARIA, ""))
			.thenThrow(new BadRequestException("Το όνομα της ομάδας είναι υποχρεωτικό."));

		assertThat(post("/api/teams", "{\"name\": \"\"}"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void joinReturnsTeamWithMembers() {
		when(teams.join(MARIA, "ABCD2345")).thenReturn(new JoinResult(TEAM, false));

		assertThat(post("/api/teams/join", "{\"inviteCode\": \"ABCD2345\"}"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.members[0].role")
			.isEqualTo("CREATOR");
	}

	@Test
	void invalidCodeIs404ProblemDetail() {
		when(teams.join(MARIA, "ZZZZ2345"))
			.thenThrow(new NotFoundException("Ο κωδικός πρόσκλησης δεν αντιστοιχεί σε ομάδα."));

		assertThat(post("/api/teams/join", "{\"inviteCode\": \"ZZZZ2345\"}"))
			.hasStatus(HttpStatus.NOT_FOUND)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void otherTeamIs404() {
		when(teams.details(new TeamId(99), MARIA)).thenThrow(new NotFoundException("Η ομάδα δεν βρέθηκε."));

		assertThat(mvc.get().uri("/api/teams/99").header(TestPlayerConfiguration.HEADER, "1"))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void withoutPlayerIs401() {
		assertThat(mvc.get().uri("/api/teams")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MockMvcTester.MockMvcRequestBuilder post(String uri, String json) {
		return mvc.post()
			.uri(uri)
			.header(TestPlayerConfiguration.HEADER, "1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json);
	}

}
