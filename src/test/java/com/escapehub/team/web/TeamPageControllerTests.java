package com.escapehub.team.web;

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

import com.escapehub.common.BadRequestException;
import com.escapehub.common.NotFoundException;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.JoinResult;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamMember;
import com.escapehub.team.application.TeamService;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.user.PlayerId;

@WebMvcTest(TeamPageController.class)
@Import(TestPlayerConfiguration.class)
class TeamPageControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final TeamDetails TEAM = new TeamDetails(new TeamId(10), "Οι Ερευνητές", "ABCD2345",
			List.of(new TeamMember(MARIA, "Μαρία", TeamRole.CREATOR)));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TeamService teams;

	@Test
	void teamsPageListsMyTeamsWithRole() {
		when(teams.teamsOf(MARIA))
			.thenReturn(List.of(new TeamSummary(new TeamId(10), "Οι Ερευνητές", TeamRole.CREATOR)));

		assertThat(mvc.get().uri("/teams").header(TestPlayerConfiguration.HEADER, "1"))
			.hasStatusOk()
			.hasViewName("team/teams")
			.bodyText()
			.contains("Οι Ερευνητές")
			.contains("Δημιουργός")
			.contains("Νέα ομάδα")
			.contains("Μπες σε ομάδα");
	}

	@Test
	void teamsPageWithoutPlayerRedirectsToNamePage() {
		assertThat(mvc.get().uri("/teams")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/player");
	}

	@Test
	void creatingTeamRedirectsToTeamPage() {
		when(teams.create(MARIA, "Οι Ερευνητές")).thenReturn(TEAM);

		assertThat(mvc.post().uri("/teams").header(TestPlayerConfiguration.HEADER, "1")
				.param("name", "Οι Ερευνητές"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/teams/10");
	}

	@Test
	void blankTeamNameShowsError() {
		when(teams.create(MARIA, ""))
			.thenThrow(new BadRequestException("Το όνομα της ομάδας είναι υποχρεωτικό."));

		assertThat(mvc.post().uri("/teams").header(TestPlayerConfiguration.HEADER, "1").param("name", ""))
			.hasStatusOk()
			.hasViewName("team/teams")
			.bodyText()
			.contains("Το όνομα της ομάδας είναι υποχρεωτικό.");
	}

	@Test
	void joiningRedirectsToTeamPage() {
		when(teams.join(MARIA, "ABCD2345")).thenReturn(new JoinResult(TEAM, false));

		assertThat(mvc.post().uri("/teams/join").header(TestPlayerConfiguration.HEADER, "1")
				.param("inviteCode", "ABCD2345"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/teams/10");
	}

	@Test
	void invalidCodeShowsError() {
		when(teams.join(MARIA, "ZZZZ2345"))
			.thenThrow(new NotFoundException("Ο κωδικός πρόσκλησης δεν αντιστοιχεί σε ομάδα."));

		assertThat(mvc.post().uri("/teams/join").header(TestPlayerConfiguration.HEADER, "1")
				.param("inviteCode", "ZZZZ2345"))
			.hasStatusOk()
			.hasViewName("team/teams")
			.bodyText()
			.contains("Ο κωδικός πρόσκλησης δεν αντιστοιχεί σε ομάδα.");
	}

	@Test
	void teamPageShowsInviteCodeAndMembers() {
		when(teams.details(new TeamId(10), MARIA)).thenReturn(TEAM);

		assertThat(mvc.get().uri("/teams/10").header(TestPlayerConfiguration.HEADER, "1"))
			.hasStatusOk()
			.hasViewName("team/team")
			.bodyText()
			.contains("ABCD2345")
			.contains("Μαρία")
			.contains("Δημιουργός");
	}

	@Test
	void teamPageOfOtherTeamIsNotFound() {
		when(teams.details(new TeamId(99), MARIA)).thenThrow(new NotFoundException("Η ομάδα δεν βρέθηκε."));

		assertThat(mvc.get().uri("/teams/99").header(TestPlayerConfiguration.HEADER, "1"))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

}
