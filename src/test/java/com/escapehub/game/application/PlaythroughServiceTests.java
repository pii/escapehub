package com.escapehub.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.game.infrastructure.PlaythroughEntity;
import com.escapehub.game.infrastructure.PlaythroughRepository;
import com.escapehub.mission.application.MissionCatalog;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamMember;
import com.escapehub.team.application.TeamService;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaythroughServiceTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final PlayerId NIKOS = new PlayerId(2);

	private static final TeamId TEAM = new TeamId(10);

	private static final Instant START = Instant.parse("2026-11-26T14:00:00Z");

	private static final Mission DIODATI = mission("villa-diodati", "Το μυστήριο της Villa Diodati");

	private static final Mission SECOND = mission("second", "Δεύτερη αποστολή");

	private static final Mission THIRD = mission("third", "Τρίτη αποστολή");

	@Mock
	private PlaythroughRepository playthroughs;

	@Mock
	private MissionCatalog missions;

	@Mock
	private TeamService teams;

	@Mock
	private PlayerService players;

	@Test
	void creatorStartsNewPlaythroughWithMissionVersion() {
		givenCreator(MARIA, TEAM);
		when(playthroughs.startIfAbsent(10, "villa-diodati", 2)).thenReturn(1);
		givenActive(entity(5, TEAM, "villa-diodati", null));

		StartResult result = service().start(MARIA, TEAM, "villa-diodati");

		assertThat(result.created()).isTrue();
		PlaythroughDetails details = result.playthrough();
		assertThat(details.id()).isEqualTo(new PlaythroughId(5));
		assertThat(details.missionVersion()).isEqualTo(2);
		assertThat(details.startedAt()).isEqualTo(START);
		assertThat(details.status()).isEqualTo(MissionStatus.IN_PROGRESS);
		assertThat(details.teamName()).isEqualTo("Οι Ερευνητές");
		assertThat(details.paths()).extracting(MissionPath::title).containsExactly("Η επιστολή", "Το βιβλίο");
	}

	@Test
	void activePlaythroughIsContinuedInsteadOfStartingSecond() {
		givenCreator(MARIA, TEAM);
		when(playthroughs.startIfAbsent(10, "villa-diodati", 2)).thenReturn(0);
		givenActive(entity(5, TEAM, "villa-diodati", null));

		StartResult result = service().start(MARIA, TEAM, "villa-diodati");

		assertThat(result.created()).isFalse();
		assertThat(result.playthrough().id()).isEqualTo(new PlaythroughId(5));
	}

	@Test
	void memberCannotStartMission() {
		when(missions.get("villa-diodati")).thenReturn(DIODATI);
		when(teams.isCreator(TEAM, NIKOS)).thenReturn(false);
		when(teams.isMember(TEAM, NIKOS)).thenReturn(true);

		assertThatThrownBy(() -> service().start(NIKOS, TEAM, "villa-diodati"))
			.isInstanceOf(ForbiddenException.class)
			.hasMessage(PlaythroughService.NOT_CREATOR);
		verify(playthroughs, never()).startIfAbsent(anyLong(), anyString(), anyInt());
	}

	@Test
	void outsiderCannotStartMission() {
		when(missions.get("villa-diodati")).thenReturn(DIODATI);

		assertThatThrownBy(() -> service().start(NIKOS, TEAM, "villa-diodati"))
			.isInstanceOf(ForbiddenException.class)
			.hasMessage(PlaythroughService.NOT_MEMBER);
		verify(playthroughs, never()).startIfAbsent(anyLong(), anyString(), anyInt());
	}

	@Test
	void unknownMissionIsNotFound() {
		when(missions.get("nope")).thenThrow(new NotFoundException("Η αποστολή δεν βρέθηκε."));

		assertThatThrownBy(() -> service().start(MARIA, TEAM, "nope")).isInstanceOf(NotFoundException.class);
		verify(playthroughs, never()).startIfAbsent(anyLong(), anyString(), anyInt());
	}

	@Test
	void soloPlayCreatesTeamOfOne() {
		TeamId solo = new TeamId(20);
		when(players.findName(MARIA)).thenReturn(Optional.of("Μαρία"));
		when(teams.teamsOf(MARIA)).thenReturn(List.of());
		when(teams.create(MARIA, "Ατομικά: Μαρία")).thenReturn(team(solo, "Ατομικά: Μαρία", 1));
		givenCreator(MARIA, solo);
		when(playthroughs.startIfAbsent(20, "villa-diodati", 2)).thenReturn(1);
		givenActive(entity(7, solo, "villa-diodati", null));

		StartResult result = service().startSolo(MARIA, "villa-diodati");

		assertThat(result.created()).isTrue();
		assertThat(result.playthrough().team()).isEqualTo(solo);
	}

	@Test
	void soloPlayReusesExistingSoloTeam() {
		TeamId solo = new TeamId(20);
		when(players.findName(MARIA)).thenReturn(Optional.of("Μαρία"));
		when(teams.teamsOf(MARIA)).thenReturn(List.of(new TeamSummary(TEAM, "Οι Ερευνητές", TeamRole.CREATOR),
				new TeamSummary(solo, "Ατομικά: Μαρία", TeamRole.CREATOR)));
		givenCreator(MARIA, solo);
		when(teams.details(solo, MARIA)).thenReturn(team(solo, "Ατομικά: Μαρία", 1));
		when(playthroughs.startIfAbsent(20, "villa-diodati", 2)).thenReturn(0);
		givenActive(entity(7, solo, "villa-diodati", null));

		StartResult result = service().startSolo(MARIA, "villa-diodati");

		assertThat(result.playthrough().team()).isEqualTo(solo);
		verify(teams, never()).create(MARIA, "Ατομικά: Μαρία");
	}

	@Test
	void soloTeamThatOthersJoinedIsNotReused() {
		TeamId shared = new TeamId(20);
		TeamId solo = new TeamId(21);
		when(players.findName(MARIA)).thenReturn(Optional.of("Μαρία"));
		when(teams.teamsOf(MARIA))
			.thenReturn(List.of(new TeamSummary(shared, "Ατομικά: Μαρία", TeamRole.CREATOR)));
		when(teams.details(shared, MARIA)).thenReturn(team(shared, "Ατομικά: Μαρία", 2));
		when(teams.create(MARIA, "Ατομικά: Μαρία")).thenReturn(team(solo, "Ατομικά: Μαρία", 1));
		givenCreator(MARIA, solo);
		when(playthroughs.startIfAbsent(21, "villa-diodati", 2)).thenReturn(1);
		givenActive(entity(8, solo, "villa-diodati", null));

		assertThat(service().startSolo(MARIA, "villa-diodati").playthrough().team()).isEqualTo(solo);
	}

	@Test
	void memberSeesPlaythrough() {
		when(playthroughs.findById(5L)).thenReturn(Optional.of(entity(5, TEAM, "villa-diodati", null)));
		when(teams.isMember(TEAM, NIKOS)).thenReturn(true);
		when(teams.details(TEAM, NIKOS)).thenReturn(team(TEAM, "Οι Ερευνητές", 2));
		when(missions.find("villa-diodati")).thenReturn(Optional.of(DIODATI));

		PlaythroughDetails details = service().details(NIKOS, new PlaythroughId(5));

		assertThat(details.missionTitle()).isEqualTo("Το μυστήριο της Villa Diodati");
	}

	@Test
	void playthroughOfOtherTeamIsForbidden() {
		when(playthroughs.findById(5L)).thenReturn(Optional.of(entity(5, TEAM, "villa-diodati", null)));

		assertThatThrownBy(() -> service().details(NIKOS, new PlaythroughId(5)))
			.isInstanceOf(ForbiddenException.class)
			.hasMessage(PlaythroughService.OTHER_TEAM);
	}

	@Test
	void unknownPlaythroughIsNotFound() {
		assertThatThrownBy(() -> service().details(MARIA, new PlaythroughId(99)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void catalogWithoutTeamHasNoStatus() {
		when(missions.all()).thenReturn(List.of(DIODATI));

		assertThat(service().catalog(MARIA, null)).singleElement().satisfies(entry -> {
			assertThat(entry.title()).isEqualTo("Το μυστήριο της Villa Diodati");
			assertThat(entry.estimatedMinutes()).isEqualTo(45);
			assertThat(entry.minPlayers()).isEqualTo(2);
			assertThat(entry.status()).isNull();
		});
	}

	@Test
	void catalogShowsStatusOfEveryMissionForTheTeam() {
		when(teams.isMember(TEAM, MARIA)).thenReturn(true);
		when(missions.all()).thenReturn(List.of(DIODATI, SECOND, THIRD));
		when(playthroughs.findByTeamId(10L)).thenReturn(List.of(entity(5, TEAM, "villa-diodati", null),
				entity(6, TEAM, "second", START.plusSeconds(600))));

		List<CatalogEntry> catalog = service().catalog(MARIA, TEAM);

		assertThat(catalog).extracting(CatalogEntry::status)
			.containsExactly(MissionStatus.IN_PROGRESS, MissionStatus.COMPLETED, MissionStatus.NOT_STARTED);
		assertThat(catalog).extracting(CatalogEntry::activePlaythrough)
			.containsExactly(new PlaythroughId(5), null, null);
	}

	@Test
	void catalogOfOtherTeamIsForbidden() {
		assertThatThrownBy(() -> service().catalog(MARIA, TEAM)).isInstanceOf(ForbiddenException.class);
	}

	private PlaythroughService service() {
		return new PlaythroughService(playthroughs, missions, teams, players);
	}

	private void givenCreator(PlayerId player, TeamId team) {
		when(missions.get("villa-diodati")).thenReturn(DIODATI);
		when(missions.find("villa-diodati")).thenReturn(Optional.of(DIODATI));
		when(teams.isCreator(team, player)).thenReturn(true);
		when(teams.isMember(team, player)).thenReturn(true);
		when(teams.details(team, player)).thenReturn(team(team, "Οι Ερευνητές", 1));
	}

	private void givenActive(PlaythroughEntity active) {
		when(playthroughs.findByTeamIdAndMissionIdAndCompletedAtIsNull(active.getTeamId(),
				active.getMissionId()))
			.thenReturn(Optional.of(active));
	}

	private static PlaythroughEntity entity(long id, TeamId team, String missionId, Instant completedAt) {
		PlaythroughEntity entity = BeanUtils.instantiateClass(PlaythroughEntity.class);
		ReflectionTestUtils.setField(entity, "id", id);
		ReflectionTestUtils.setField(entity, "teamId", team.value());
		ReflectionTestUtils.setField(entity, "missionId", missionId);
		ReflectionTestUtils.setField(entity, "missionVersion", 2);
		ReflectionTestUtils.setField(entity, "startedAt", START);
		ReflectionTestUtils.setField(entity, "completedAt", completedAt);
		return entity;
	}

	private static TeamDetails team(TeamId id, String name, int size) {
		List<TeamMember> members = List.of(new TeamMember(MARIA, "Μαρία", TeamRole.CREATOR),
				new TeamMember(NIKOS, "Νίκος", TeamRole.MEMBER));
		return new TeamDetails(id, name, "ABCD2345", members.subList(0, size));
	}

	private static Mission mission(String id, String title) {
		List<MissionPath> paths = List.of(new MissionPath("letter", "Η επιστολή"),
				new MissionPath("book", "Το βιβλίο"));
		return new Mission(id, 2, title, "Περίληψη", 2, 45, paths, List.of(), List.of());
	}

}
