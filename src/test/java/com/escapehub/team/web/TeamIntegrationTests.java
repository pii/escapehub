package com.escapehub.team.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.support.IntegrationTest;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.JoinResult;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Ομάδες από άκρη σε άκρη, με πραγματική PostgreSQL. Οι παίκτες δημιουργούνται με το {@link PlayerService} και ο
 * τρέχων παίκτης ορίζεται με το header του {@link TestPlayerConfiguration}. Κάθε έλεγχος έχει δικά του ονόματα,
 * γιατί η βάση είναι κοινή για όλους τους ελέγχους.
 */
@IntegrationTest
@Import(TestPlayerConfiguration.class)
class TeamIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private TeamService teams;

	@Autowired
	private PlayerService players;

	@Test
	void creatorAndJoinedMemberSeeTheTeam() {
		PlayerId creator = player("TeamCreator1");
		PlayerId member = player("TeamMember1");
		TeamDetails team = teams.create(creator, "Οι Ερευνητές");

		assertThat(post(member, "/api/teams/join", "{\"inviteCode\": \"" + team.inviteCode() + "\"}"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.members.length()")
			.isEqualTo(2);

		TeamDetails seen = teams.details(team.id(), member);
		assertThat(seen.members()).extracting(m -> m.role())
			.containsExactly(TeamRole.CREATOR, TeamRole.MEMBER);
		assertThat(teams.isCreator(team.id(), creator)).isTrue();
		assertThat(teams.isCreator(team.id(), member)).isFalse();
		assertThat(teams.isMember(team.id(), member)).isTrue();
	}

	@Test
	void joiningTwiceDoesNotAddPlayerTwice() {
		PlayerId creator = player("TeamCreator2");
		PlayerId member = player("TeamMember2");
		TeamDetails team = teams.create(creator, "Δύο φορές");

		JoinResult first = teams.join(member, team.inviteCode());
		JoinResult second = teams.join(member, team.inviteCode().toLowerCase());

		assertThat(first.alreadyMember()).isFalse();
		assertThat(second.alreadyMember()).isTrue();
		assertThat(second.team().members()).hasSize(2);
	}

	@Test
	void concurrentJoinsOfSamePlayerCreateOneMembership() throws Exception {
		PlayerId creator = player("TeamCreator3");
		PlayerId member = player("TeamMember3");
		TeamDetails team = teams.create(creator, "Ταυτόχρονα");

		CountDownLatch start = new CountDownLatch(1);
		List<Future<JoinResult>> attempts = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
			for (int i = 0; i < 4; i++) {
				attempts.add(executor.submit(() -> {
					start.await();
					return teams.join(member, team.inviteCode());
				}));
			}
			start.countDown();
		}

		int added = 0;
		for (Future<JoinResult> attempt : attempts) {
			if (!attempt.get().alreadyMember()) {
				added++;
			}
		}
		assertThat(added).isEqualTo(1);
		assertThat(teams.details(team.id(), creator).members()).hasSize(2);
	}

	@Test
	void nonMemberCannotSeeTeam() {
		PlayerId creator = player("TeamCreator4");
		PlayerId outsider = player("TeamOutsider4");
		TeamDetails team = teams.create(creator, "Μυστική ομάδα");

		assertThat(get(outsider, "/api/teams/" + team.id().value())).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get(outsider, "/teams/" + team.id().value())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void invalidCodeIsNotFound() {
		PlayerId member = player("TeamMember5");

		assertThat(post(member, "/api/teams/join", "{\"inviteCode\": \"ZZZZ2345\"}"))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void pagesShowTeamCodeAndMembers() {
		PlayerId creator = player("TeamCreator6");
		TeamDetails team = teams.create(creator, "Η ομάδα της σελίδας");

		assertThat(get(creator, "/teams")).hasStatusOk().bodyText().contains("Η ομάδα της σελίδας");
		assertThat(get(creator, "/teams/" + team.id().value()))
			.hasStatusOk()
			.bodyText()
			.contains(team.inviteCode())
			.contains("TeamCreator6");
	}

	private PlayerId player(String name) {
		return players.register(name).id();
	}

	private MockMvcTester.MockMvcRequestBuilder get(PlayerId player, String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()));
	}

	private MockMvcTester.MockMvcRequestBuilder post(PlayerId player, String uri, String json) {
		return mvc.post()
			.uri(uri)
			.header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json);
	}

}
