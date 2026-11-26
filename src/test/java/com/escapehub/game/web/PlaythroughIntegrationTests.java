package com.escapehub.game.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.PlaythroughDetails;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.application.StartResult;
import com.escapehub.mission.application.MissionCatalog;
import com.escapehub.mission.domain.Puzzle;
import com.escapehub.support.IntegrationTest;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Κατάλογος και έναρξη αποστολής από άκρη σε άκρη (#27), με πραγματική PostgreSQL και την αποστολή
 * {@code villa-diodati} των αρχείων. Κάθε έλεγχος έχει δικά του ονόματα παικτών, γιατί η βάση είναι κοινή.
 */
@IntegrationTest
@Import(TestPlayerConfiguration.class)
class PlaythroughIntegrationTests {

	private static final String MISSION = "villa-diodati";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private PlaythroughService playthroughs;

	@Autowired
	private TeamService teams;

	@Autowired
	private PlayerService players;

	@Autowired
	private MissionCatalog missions;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void creatorStartsMissionAndStartTimeIsRecorded() {
		PlayerId creator = player("PtCreator1");
		TeamDetails team = teams.create(creator, "Ομάδα έναρξης");

		MvcTestResult result = start(creator, team).exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.missionVersion")
			.isEqualTo(1);
		PlaythroughDetails started = playthroughs.details(creator, idOf(result));
		assertThat(started.startedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
		assertThat(started.team()).isEqualTo(team.id());
	}

	@Test
	void secondStartContinuesTheActivePlaythrough() {
		PlayerId creator = player("PtCreator2");
		TeamDetails team = teams.create(creator, "Ομάδα συνέχειας");

		PlaythroughId first = idOf(start(creator, team).exchange());
		MvcTestResult second = start(creator, team).exchange();

		assertThat(second).hasStatusOk();
		assertThat(idOf(second)).isEqualTo(first);
		assertThat(countPlaythroughs(team)).isEqualTo(1);
	}

	@Test
	void memberCannotStartMission() {
		PlayerId creator = player("PtCreator3");
		PlayerId member = player("PtMember3");
		TeamDetails team = teams.create(creator, "Ομάδα μελών");
		teams.join(member, team.inviteCode());

		assertThat(start(member, team)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(page(member, "/playthroughs").param("teamId", String.valueOf(team.id().value()))
				.param("missionId", MISSION))
			.hasRedirectedUrl("/missions?team=" + team.id().value())
			.flash()
			.containsKey("error");
		assertThat(countPlaythroughs(team)).isZero();
	}

	@Test
	void onlyTeamMembersSeeThePlaythrough() {
		PlayerId creator = player("PtCreator4");
		PlayerId member = player("PtMember4");
		PlayerId outsider = player("PtOutsider4");
		TeamDetails team = teams.create(creator, "Ομάδα προβολής");
		teams.join(member, team.inviteCode());
		long id = idOf(start(creator, team).exchange()).value();

		assertThat(get(member, "/api/playthroughs/" + id)).hasStatusOk();
		assertThat(get(member, "/playthroughs/" + id)).hasStatusOk().bodyText().contains("Η επιστολή");
		assertThat(get(outsider, "/api/playthroughs/" + id)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(get(outsider, "/playthroughs/" + id)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void soloPlayUsesTeamOfOneAndContinuesOnSecondStart() {
		PlayerId solo = player("PtSolo5");
		String body = "{\"missionId\": \"" + MISSION + "\", \"solo\": true}";

		MvcTestResult first = post(solo, "/api/playthroughs", body).exchange();
		MvcTestResult second = post(solo, "/api/playthroughs", body).exchange();

		assertThat(first).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.teamName")
			.isEqualTo("Ατομικά: PtSolo5");
		assertThat(second).hasStatusOk();
		assertThat(idOf(second)).isEqualTo(idOf(first));
		PlaythroughDetails details = playthroughs.details(solo, idOf(first));
		assertThat(teams.details(details.team(), solo).members()).hasSize(1);
	}

	@Test
	void concurrentStartsCreateOnePlaythrough() throws Exception {
		PlayerId creator = player("PtCreator6");
		TeamDetails team = teams.create(creator, "Ταυτόχρονη έναρξη");

		CountDownLatch go = new CountDownLatch(1);
		List<Future<StartResult>> attempts = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
			for (int i = 0; i < 4; i++) {
				attempts.add(executor.submit(() -> {
					go.await();
					return playthroughs.start(creator, team.id(), MISSION);
				}));
			}
			go.countDown();
		}

		List<StartResult> results = new ArrayList<>();
		for (Future<StartResult> attempt : attempts) {
			results.add(attempt.get());
		}
		assertThat(results).filteredOn(StartResult::created).hasSize(1);
		assertThat(results).extracting(result -> result.playthrough().id()).containsOnly(results.get(0)
			.playthrough()
			.id());
		assertThat(countPlaythroughs(team)).isEqualTo(1);
	}

	@Test
	void catalogFromMissionFilesShowsStatusForTheTeam() {
		PlayerId creator = player("PtCreator7");
		TeamDetails team = teams.create(creator, "Ομάδα καταλόγου");
		String catalog = "/api/missions?teamId=" + team.id().value();

		assertThat(get(creator, catalog)).hasStatusOk()
			.bodyJson()
			.extractingPath("$[?(@.id == 'villa-diodati')].status")
			.asArray()
			.containsExactly("NOT_STARTED");

		PlaythroughId first = idOf(start(creator, team).exchange());
		assertThat(get(creator, catalog)).bodyJson()
			.extractingPath("$[?(@.id == 'villa-diodati')].playthroughId")
			.asArray()
			.containsExactly((int) first.value());

		jdbc.update("update playthrough set completed_at = now() where id = ?", first.value());
		assertThat(get(creator, catalog)).bodyJson()
			.extractingPath("$[?(@.id == 'villa-diodati')].status")
			.asArray()
			.containsExactly("COMPLETED");

		MvcTestResult again = start(creator, team).exchange();
		assertThat(again).hasStatus(HttpStatus.CREATED);
		assertThat(idOf(again)).isNotEqualTo(first);
	}

	@Test
	void catalogPageListsTheMissionOfTheFiles() {
		PlayerId player = player("PtViewer8");

		assertThat(get(player, "/missions")).hasStatusOk()
			.bodyText()
			.contains("Το μυστήριο της Villa Diodati")
			.contains("Παίξε ατομικά");
	}

	@Test
	void responsesDoNotContainAnswers() {
		PlayerId creator = player("PtCreator9");
		TeamDetails team = teams.create(creator, "Ομάδα απαντήσεων");
		long id = idOf(start(creator, team).exchange()).value();
		List<String> answers = new ArrayList<>();
		for (Puzzle puzzle : missions.get(MISSION).puzzles()) {
			answers.addAll(puzzle.answer().accepted());
			puzzle.answer().values().values().forEach(answers::addAll);
		}

		for (String uri : List.of("/api/missions?teamId=" + team.id().value(), "/api/playthroughs/" + id,
				"/missions", "/playthroughs/" + id)) {
			assertThat(get(creator, uri)).as(uri).hasStatusOk().bodyText().doesNotContain(answers);
		}
	}

	private PlayerId player(String name) {
		return players.register(name).id();
	}

	private int countPlaythroughs(TeamDetails team) {
		Integer count = jdbc.queryForObject("select count(*) from playthrough where team_id = ?", Integer.class,
				team.id().value());
		return (count != null) ? count : 0;
	}

	private static PlaythroughId idOf(MvcTestResult result) {
		try {
			String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			Number id = JsonPath.read(body, "$.id");
			return new PlaythroughId(id.longValue());
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private MockMvcTester.MockMvcRequestBuilder start(PlayerId player, TeamDetails team) {
		return post(player, "/api/playthroughs",
				"{\"missionId\": \"" + MISSION + "\", \"teamId\": " + team.id().value() + "}");
	}

	private MockMvcTester.MockMvcRequestBuilder get(PlayerId player, String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()));
	}

	private MockMvcTester.MockMvcRequestBuilder page(PlayerId player, String uri) {
		return mvc.post().uri(uri).header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()));
	}

	private MockMvcTester.MockMvcRequestBuilder post(PlayerId player, String uri, String json) {
		return page(player, uri).contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
