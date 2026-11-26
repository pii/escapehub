package com.escapehub.team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.escapehub.common.BadRequestException;
import com.escapehub.common.NotFoundException;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.infrastructure.TeamEntity;
import com.escapehub.team.infrastructure.TeamMembershipEntity;
import com.escapehub.team.infrastructure.TeamMembershipRepository;
import com.escapehub.team.infrastructure.TeamRepository;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final PlayerId NIKOS = new PlayerId(2);

	@Mock
	private TeamRepository teams;

	@Mock
	private TeamMembershipRepository memberships;

	@Mock
	private PlayerService players;

	@Test
	void creatorBecomesCreatorMemberOfNewTeam() {
		when(teams.existsByInviteCode(anyString())).thenReturn(false);
		when(teams.saveAndFlush(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));
		givenTeam(10L, "Οι Ερευνητές", "ABCD2345");
		givenMembers(10L, member(10L, MARIA, TeamRole.CREATOR));

		TeamDetails team = service().create(MARIA, "  Οι Ερευνητές ");

		verify(memberships).saveIfAbsent(10L, 1L, "CREATOR");
		assertThat(team.name()).isEqualTo("Οι Ερευνητές");
		assertThat(team.members()).singleElement()
			.satisfies(member -> assertThat(member.role()).isEqualTo(TeamRole.CREATOR));
	}

	@Test
	void blankTeamNameIsRejectedWithoutSaving() {
		assertThatThrownBy(() -> service().create(MARIA, "   ")).isInstanceOf(BadRequestException.class);
		verify(teams, never()).saveAndFlush(any());
	}

	@Test
	void validCodeAddsPlayerAsMember() {
		givenTeam(10L, "Οι Ερευνητές", "ABCD2345");
		when(memberships.saveIfAbsent(10L, 2L, "MEMBER")).thenReturn(1);
		givenMembers(10L, member(10L, MARIA, TeamRole.CREATOR), member(10L, NIKOS, TeamRole.MEMBER));

		JoinResult result = service().join(NIKOS, " abcd2345 ");

		assertThat(result.alreadyMember()).isFalse();
		assertThat(result.team().members()).hasSize(2);
	}

	@Test
	void joiningAgainDoesNotAddPlayerTwice() {
		givenTeam(10L, "Οι Ερευνητές", "ABCD2345");
		when(memberships.saveIfAbsent(10L, 2L, "MEMBER")).thenReturn(0);
		givenMembers(10L, member(10L, MARIA, TeamRole.CREATOR), member(10L, NIKOS, TeamRole.MEMBER));

		JoinResult result = service().join(NIKOS, "ABCD2345");

		assertThat(result.alreadyMember()).isTrue();
	}

	@Test
	void unknownCodeIsNotFound() {
		when(teams.findByInviteCode("ZZZZ2345")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().join(NIKOS, "ZZZZ2345"))
			.isInstanceOf(NotFoundException.class)
			.hasMessage(TeamService.INVALID_CODE);
	}

	@Test
	void malformedCodeIsNotFoundWithoutDatabaseLookup() {
		assertThatThrownBy(() -> service().join(NIKOS, "not-a-code")).isInstanceOf(NotFoundException.class);
		verifyNoInteractions(teams);
	}

	@Test
	void blankCodeIsBadRequest() {
		assertThatThrownBy(() -> service().join(NIKOS, " ")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void nonMemberCannotSeeTeam() {
		when(memberships.existsByTeamIdAndPlayerId(10L, 2L)).thenReturn(false);

		assertThatThrownBy(() -> service().details(new TeamId(10), NIKOS))
			.isInstanceOf(NotFoundException.class)
			.hasMessage(TeamService.TEAM_NOT_FOUND);
	}

	@Test
	void creatorCheckUsesRole() {
		when(memberships.existsByTeamIdAndPlayerIdAndRole(10L, 1L, TeamRole.CREATOR)).thenReturn(true);

		assertThat(service().isCreator(new TeamId(10), MARIA)).isTrue();
		assertThat(service().isCreator(new TeamId(10), NIKOS)).isFalse();
	}

	@Test
	void teamsOfListsTeamsWithRole() {
		TeamMembershipEntity membership = member(10L, NIKOS, TeamRole.MEMBER);
		when(memberships.findByPlayerIdOrderByJoinedAtAscIdAsc(2L)).thenReturn(List.of(membership));
		when(teams.findAllById(List.of(10L))).thenReturn(List.of(team(10L, "Οι Ερευνητές", "ABCD2345")));

		assertThat(service().teamsOf(NIKOS))
			.containsExactly(new TeamSummary(new TeamId(10), "Οι Ερευνητές", TeamRole.MEMBER));
	}

	private TeamService service() {
		return new TeamService(teams, memberships, players);
	}

	private void givenTeam(long id, String name, String code) {
		TeamEntity team = team(id, name, code);
		when(teams.findByInviteCode(code)).thenReturn(Optional.of(team));
		when(teams.findById(id)).thenReturn(Optional.of(team));
	}

	private void givenMembers(long teamId, TeamMembershipEntity... members) {
		for (TeamMembershipEntity member : members) {
			when(memberships.existsByTeamIdAndPlayerId(teamId, member.getPlayerId())).thenReturn(true);
			when(players.findName(new PlayerId(member.getPlayerId()))).thenReturn(Optional.of("Παίκτης"));
		}
		when(memberships.findByTeamIdOrderByJoinedAtAscIdAsc(teamId)).thenReturn(List.of(members));
	}

	private static TeamEntity team(long id, String name, String code) {
		return withId(new TeamEntity(name, code, 1L), id);
	}

	private static TeamEntity withId(TeamEntity team, long id) {
		ReflectionTestUtils.setField(team, "id", id);
		return team;
	}

	private static TeamMembershipEntity member(long teamId, PlayerId player, TeamRole role) {
		TeamMembershipEntity member = mock(TeamMembershipEntity.class);
		when(member.getTeamId()).thenReturn(teamId);
		when(member.getPlayerId()).thenReturn(player.value());
		when(member.getRole()).thenReturn(role);
		return member;
	}

}
