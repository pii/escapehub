package com.escapehub.game.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.EvidenceService;
import com.escapehub.game.application.ExamineResult;
import com.escapehub.game.application.JournalItem;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.support.IntegrationTest;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamService;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

/**
 * Το κοινό ημερολόγιο από άκρη σε άκρη (#28), με πραγματική PostgreSQL και την αποστολή {@code villa-diodati}.
 * Διαθέσιμα από την αρχή είναι τα {@code sealed-letter}, {@code portrait-back} και {@code book-spine}. Το
 * {@code letter-1816} το αποκαλύπτει γρίφος. Κάθε έλεγχος έχει δικά του ονόματα παικτών, γιατί η βάση είναι κοινή.
 */
@IntegrationTest
@Import(TestPlayerConfiguration.class)
class JournalIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private PlaythroughService playthroughs;

	@Autowired
	private EvidenceService evidence;

	@Autowired
	private TeamService teams;

	@Autowired
	private PlayerService players;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void examiningObjectRecordsWhoFoundItAndWhen() {
		Game game = game(1);

		assertThat(add(game.member(), game, "sealed-letter")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.addedBy")
			.isEqualTo("JrMember1");
		JournalItem item = evidence.journal(game.creator(), game.id()).get(0);
		assertThat(item.addedBy()).isEqualTo(game.member());
		assertThat(item.addedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void allMembersSeeTheSameJournalNewestFirst() {
		Game game = game(2);
		evidence.examine(game.creator(), game.id(), "sealed-letter");
		evidence.examine(game.member(), game.id(), "portrait-back");

		for (PlayerId viewer : List.of(game.creator(), game.member())) {
			assertThat(get(viewer, journalUri(game))).hasStatusOk()
				.bodyJson()
				.extractingPath("$[*].evidenceId")
				.asArray()
				.containsExactly("portrait-back", "sealed-letter");
		}
	}

	@Test
	void sameEvidenceIsRecordedOnce() {
		Game game = game(3);

		assertThat(add(game.creator(), game, "sealed-letter")).hasStatus(HttpStatus.CREATED);
		assertThat(add(game.member(), game, "sealed-letter")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.addedBy")
			.isEqualTo("JrCreator3");
		assertThat(entries(game)).isEqualTo(1);
	}

	@Test
	void concurrentAddsOfSameEvidenceCreateOneEntryWithoutErrors() throws Exception {
		Game game = game(4);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<ExamineResult>> attempts = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
			for (int i = 0; i < 4; i++) {
				PlayerId player = (i % 2 == 0) ? game.creator() : game.member();
				attempts.add(executor.submit(() -> {
					go.await();
					return evidence.examine(player, game.id(), "book-spine");
				}));
			}
			go.countDown();
		}

		List<ExamineResult> results = new ArrayList<>();
		for (Future<ExamineResult> attempt : attempts) {
			results.add(attempt.get());
		}
		assertThat(results).filteredOn(ExamineResult::added).hasSize(1);
		assertThat(entries(game)).isEqualTo(1);
	}

	@Test
	void evidenceNotRevealedYetIsRejectedAndJournalIsUnchanged() {
		Game game = game(5);

		assertThat(add(game.creator(), game, "letter-1816")).hasStatus(HttpStatus.CONFLICT);
		assertThat(add(game.creator(), game, "no-such-evidence")).hasStatus(HttpStatus.CONFLICT);
		assertThat(page(game.creator(), game).param("evidenceId", "letter-1816"))
			.hasRedirectedUrl("/playthroughs/" + game.id().value())
			.flash()
			.containsKey("error");
		assertThat(entries(game)).isZero();
	}

	@Test
	void onlyTeamMembersSeeAndChangeTheJournal() {
		Game game = game(6);
		PlayerId outsider = players.register("JrOutsider6").id();

		assertThat(get(outsider, journalUri(game))).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(add(outsider, game, "sealed-letter")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(page(outsider, game).param("evidenceId", "sealed-letter")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(entries(game)).isZero();
	}

	@Test
	void missionPageShowsObjectsAndTheJournal() {
		Game game = game(7);

		assertThat(page(game.creator(), game).param("evidenceId", "sealed-letter"))
			.hasRedirectedUrl("/playthroughs/" + game.id().value())
			.flash()
			.containsEntry("message", GamePageController.ADDED);

		assertThat(get(game.member(), "/playthroughs/" + game.id().value())).hasStatusOk()
			.bodyText()
			.contains("Εξέτασε")
			.contains("Στο ημερολόγιο")
			.contains("Σφραγισμένη επιστολή")
			.contains("JrCreator7");
	}

	/**
	 * Ομάδα με δημιουργό και ένα μέλος, και ενεργό playthrough της {@code villa-diodati}.
	 */
	private Game game(int n) {
		PlayerId creator = players.register("JrCreator" + n).id();
		PlayerId member = players.register("JrMember" + n).id();
		TeamDetails team = teams.create(creator, "Ομάδα ημερολογίου " + n);
		teams.join(member, team.inviteCode());
		PlaythroughId id = playthroughs.start(creator, team.id(), "villa-diodati").playthrough().id();
		return new Game(id, creator, member);
	}

	private int entries(Game game) {
		Integer count = jdbc.queryForObject("select count(*) from journal_entry where playthrough_id = ?",
				Integer.class, game.id().value());
		return (count != null) ? count : 0;
	}

	private static String journalUri(Game game) {
		return "/api/playthroughs/" + game.id().value() + "/journal";
	}

	private MockMvcTester.MockMvcRequestBuilder add(PlayerId player, Game game, String evidenceId) {
		return mvc.post()
			.uri(journalUri(game))
			.header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"evidenceId\": \"" + evidenceId + "\"}");
	}

	private MockMvcTester.MockMvcRequestBuilder page(PlayerId player, Game game) {
		return mvc.post()
			.uri("/playthroughs/" + game.id().value() + "/journal")
			.header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()));
	}

	private MockMvcTester.MockMvcRequestBuilder get(PlayerId player, String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, String.valueOf(player.value()));
	}

	private record Game(PlaythroughId id, PlayerId creator, PlayerId member) {
	}

}
