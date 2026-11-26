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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.escapehub.common.ConflictException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.CatalogEntry;
import com.escapehub.game.application.EvidenceService;
import com.escapehub.game.application.ExaminableEvidence;
import com.escapehub.game.application.ExamineResult;
import com.escapehub.game.application.JournalItem;
import com.escapehub.game.application.PlaythroughDetails;
import com.escapehub.game.application.PlaythroughService;
import com.escapehub.game.application.StartResult;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;
import com.escapehub.team.application.TeamService;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.user.PlayerId;

@WebMvcTest(GamePageController.class)
@Import(TestPlayerConfiguration.class)
class GamePageControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final TeamId TEAM = new TeamId(10);

	private static final PlaythroughDetails PLAYTHROUGH = new PlaythroughDetails(new PlaythroughId(5), TEAM,
			"Οι Ερευνητές", "villa-diodati", 1, "Το μυστήριο της Villa Diodati",
			List.of(new MissionPath("letter", "Η επιστολή"), new MissionPath("book", "Το βιβλίο")),
			Instant.parse("2026-11-26T14:00:00Z"), null, MissionStatus.IN_PROGRESS);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlaythroughService playthroughs;

	@MockitoBean
	private TeamService teams;

	@MockitoBean
	private EvidenceService evidence;

	@Test
	void playthroughPageShowsObjectsToExamineAndJournalNewestFirst() {
		when(playthroughs.details(MARIA, new PlaythroughId(5))).thenReturn(PLAYTHROUGH);
		when(evidence.examinable(MARIA, new PlaythroughId(5))).thenReturn(List.of(
				new ExaminableEvidence("sealed-letter", "Σφραγισμένη επιστολή", "Η επιστολή", true),
				new ExaminableEvidence("book-spine", "Φθαρμένο βιβλίο", "Το βιβλίο", false)));
		when(evidence.journal(MARIA, new PlaythroughId(5))).thenReturn(List.of(
				new JournalItem("portrait-back", "Επιγραφή του πορτρέτου", new PlayerId(2), "Νίκος",
						Instant.parse("2026-11-26T14:05:00Z")),
				new JournalItem("sealed-letter", "Σφραγισμένη επιστολή", MARIA, "Μαρία",
						Instant.parse("2026-11-26T14:01:00Z"))));

		assertThat(get("/playthroughs/5")).hasStatusOk()
			.bodyText()
			.contains("Φθαρμένο βιβλίο")
			.contains("Εξέτασε")
			.contains("Στο ημερολόγιο")
			.containsSubsequence("Επιγραφή του πορτρέτου", "Νίκος", "26/11/2026 16:05",
					"Σφραγισμένη επιστολή", "Μαρία", "26/11/2026 16:01");
	}

	@Test
	void examiningObjectRedirectsBackWithMessage() {
		when(evidence.examine(MARIA, new PlaythroughId(5), "book-spine"))
			.thenReturn(new ExamineResult(new JournalItem("book-spine", "Φθαρμένο βιβλίο", MARIA, "Μαρία",
					Instant.parse("2026-11-26T14:01:00Z")), true));

		assertThat(post("/playthroughs/5/journal").param("evidenceId", "book-spine"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/playthroughs/5")
			.flash()
			.containsEntry("message", GamePageController.ADDED);
	}

	@Test
	void examiningHiddenEvidenceShowsError() {
		when(evidence.examine(MARIA, new PlaythroughId(5), "letter-1816"))
			.thenThrow(new ConflictException("Το στοιχείο δεν έχει αποκαλυφθεί ακόμη."));

		assertThat(post("/playthroughs/5/journal").param("evidenceId", "letter-1816"))
			.hasRedirectedUrl("/playthroughs/5")
			.flash()
			.containsEntry("error", "Το στοιχείο δεν έχει αποκαλυφθεί ακόμη.");
	}

	@Test
	void catalogPageShowsMissionsWithStatusForFirstTeam() {
		when(teams.teamsOf(MARIA)).thenReturn(List.of(new TeamSummary(TEAM, "Οι Ερευνητές", TeamRole.CREATOR)));
		when(playthroughs.catalog(MARIA, TEAM)).thenReturn(List.of(
				entry("villa-diodati", "Το μυστήριο της Villa Diodati", MissionStatus.IN_PROGRESS, 5L),
				entry("second", "Δεύτερη αποστολή", MissionStatus.NOT_STARTED, null)));

		assertThat(get("/missions")).hasStatusOk()
			.hasViewName("game/missions")
			.bodyText()
			.contains("Το μυστήριο της Villa Diodati")
			.contains("Ένα κλειδωμένο αρχείο.")
			.contains("Περίπου 45 λεπτά")
			.contains("Τουλάχιστον 2 παίκτες")
			.contains("Οι Ερευνητές: Σε εξέλιξη")
			.contains("Οι Ερευνητές: Δεν έχει ξεκινήσει")
			.contains("Συνέχεια")
			.contains("Έναρξη")
			.contains("Παίξε ατομικά");
	}

	@Test
	void catalogPageWithoutTeamOffersSoloPlay() {
		when(teams.teamsOf(MARIA)).thenReturn(List.of());
		when(playthroughs.catalog(MARIA, null))
			.thenReturn(List.of(entry("villa-diodati", "Το μυστήριο της Villa Diodati", null, null)));

		assertThat(get("/missions")).hasStatusOk()
			.bodyText()
			.contains("Δεν ανήκεις ακόμη σε ομάδα")
			.contains("Παίξε ατομικά")
			.doesNotContain("Έναρξη");
	}

	@Test
	void catalogPageOfOtherTeamIs403() {
		when(teams.teamsOf(MARIA)).thenReturn(List.of(new TeamSummary(TEAM, "Οι Ερευνητές", TeamRole.CREATOR)));

		assertThat(get("/missions?team=99")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void startRedirectsToPlaythrough() {
		when(playthroughs.start(MARIA, TEAM, "villa-diodati")).thenReturn(new StartResult(PLAYTHROUGH, true));

		assertThat(post("/playthroughs").param("teamId", "10").param("missionId", "villa-diodati"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/playthroughs/5")
			.flash()
			.containsEntry("message", GamePageController.STARTED);
	}

	@Test
	void startWithActivePlaythroughContinuesIt() {
		when(playthroughs.start(MARIA, TEAM, "villa-diodati")).thenReturn(new StartResult(PLAYTHROUGH, false));

		assertThat(post("/playthroughs").param("teamId", "10").param("missionId", "villa-diodati"))
			.hasRedirectedUrl("/playthroughs/5")
			.flash()
			.containsEntry("message", GamePageController.CONTINUED);
	}

	@Test
	void memberWhoStartsSeesMessageInCatalog() {
		String onlyCreator = "Μόνο ο δημιουργός της ομάδας μπορεί να ξεκινήσει αποστολή.";
		when(playthroughs.start(MARIA, TEAM, "villa-diodati")).thenThrow(new ForbiddenException(onlyCreator));

		assertThat(post("/playthroughs").param("teamId", "10").param("missionId", "villa-diodati"))
			.hasStatus(HttpStatus.FOUND)
			.hasRedirectedUrl("/missions?team=10")
			.flash()
			.containsEntry("error", onlyCreator);
	}

	@Test
	void soloStartRedirectsToPlaythrough() {
		when(playthroughs.startSolo(MARIA, "villa-diodati")).thenReturn(new StartResult(PLAYTHROUGH, true));

		assertThat(post("/playthroughs/solo").param("missionId", "villa-diodati"))
			.hasRedirectedUrl("/playthroughs/5");
	}

	@Test
	void playthroughPageShowsTitleStartTimeAndPaths() {
		when(playthroughs.details(MARIA, new PlaythroughId(5))).thenReturn(PLAYTHROUGH);

		assertThat(get("/playthroughs/5")).hasStatusOk()
			.hasViewName("game/playthrough")
			.bodyText()
			.contains("Το μυστήριο της Villa Diodati")
			.contains("Οι Ερευνητές")
			.contains("26/11/2026 16:00")
			.contains("Σε εξέλιξη")
			.contains("Η επιστολή")
			.contains("Το βιβλίο");
	}

	@Test
	void playthroughPageOfOtherTeamIs403() {
		when(playthroughs.details(MARIA, new PlaythroughId(5)))
			.thenThrow(new ForbiddenException("Το παιχνίδι ανήκει σε άλλη ομάδα."));

		assertThat(get("/playthroughs/5")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void pagesWithoutPlayerRedirectToNamePage() {
		assertThat(mvc.get().uri("/missions")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/player");
		assertThat(mvc.get().uri("/playthroughs/5")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/player");
	}

	private MockMvcTester.MockMvcRequestBuilder get(String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, "1");
	}

	private MockMvcTester.MockMvcRequestBuilder post(String uri) {
		return mvc.post().uri(uri).header(TestPlayerConfiguration.HEADER, "1");
	}

	private static CatalogEntry entry(String id, String title, MissionStatus status, Long active) {
		PlaythroughId playthrough = (active != null) ? new PlaythroughId(active) : null;
		return new CatalogEntry(id, title, "Ένα κλειδωμένο αρχείο.", 45, 2, status, playthrough);
	}

}
