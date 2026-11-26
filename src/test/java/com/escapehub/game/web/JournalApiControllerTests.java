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

import com.escapehub.common.ConflictException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.common.NotFoundException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.application.EvidenceService;
import com.escapehub.game.application.ExamineResult;
import com.escapehub.game.application.JournalItem;
import com.escapehub.support.TestPlayerConfiguration;
import com.escapehub.user.PlayerId;

@WebMvcTest(JournalApiController.class)
@Import(TestPlayerConfiguration.class)
class JournalApiControllerTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final PlaythroughId PLAYTHROUGH = new PlaythroughId(5);

	private static final JournalItem SEALED = new JournalItem("sealed-letter", "Σφραγισμένη επιστολή", MARIA,
			"Μαρία", Instant.parse("2026-11-26T14:00:00Z"));

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private EvidenceService evidence;

	@Test
	void addReturns201WithWhoFoundItAndWhen() {
		when(evidence.examine(MARIA, PLAYTHROUGH, "sealed-letter")).thenReturn(new ExamineResult(SEALED, true));

		var json = assertThat(post("{\"evidenceId\": \"sealed-letter\"}")).hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "/api/playthroughs/5/journal")
			.bodyJson();
		json.extractingPath("$.title").isEqualTo("Σφραγισμένη επιστολή");
		json.extractingPath("$.addedBy").isEqualTo("Μαρία");
		json.extractingPath("$.addedById").isEqualTo(1);
		json.extractingPath("$.addedAt").isEqualTo("2026-11-26T14:00:00Z");
	}

	@Test
	void addingEvidenceAlreadyInJournalReturns200() {
		when(evidence.examine(MARIA, PLAYTHROUGH, "sealed-letter"))
			.thenReturn(new ExamineResult(SEALED, false));

		assertThat(post("{\"evidenceId\": \"sealed-letter\"}")).hasStatusOk();
	}

	@Test
	void evidenceNotRevealedIs409ProblemDetail() {
		when(evidence.examine(MARIA, PLAYTHROUGH, "letter-1816"))
			.thenThrow(new ConflictException("Το στοιχείο δεν έχει αποκαλυφθεί ακόμη."));

		assertThat(post("{\"evidenceId\": \"letter-1816\"}")).hasStatus(HttpStatus.CONFLICT)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Το στοιχείο δεν έχει αποκαλυφθεί ακόμη.");
	}

	@Test
	void blankEvidenceIs400() {
		assertThat(post("{\"evidenceId\": \"\"}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void journalListsEntriesInServiceOrder() {
		JournalItem newer = new JournalItem("portrait-back", "Επιγραφή", new PlayerId(2), "Νίκος",
				Instant.parse("2026-11-26T14:05:00Z"));
		when(evidence.journal(MARIA, PLAYTHROUGH)).thenReturn(List.of(newer, SEALED));

		var json = assertThat(get("/api/playthroughs/5/journal")).hasStatusOk().bodyJson();
		json.extractingPath("$[0].evidenceId").isEqualTo("portrait-back");
		json.extractingPath("$[1].addedBy").isEqualTo("Μαρία");
	}

	@Test
	void outsiderIs403() {
		when(evidence.journal(MARIA, PLAYTHROUGH)).thenThrow(new ForbiddenException("Άλλη ομάδα."));
		when(evidence.examine(MARIA, PLAYTHROUGH, "sealed-letter"))
			.thenThrow(new ForbiddenException("Άλλη ομάδα."));

		assertThat(get("/api/playthroughs/5/journal")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(post("{\"evidenceId\": \"sealed-letter\"}")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void unknownPlaythroughIs404() {
		when(evidence.journal(MARIA, new PlaythroughId(99))).thenThrow(new NotFoundException("Δεν βρέθηκε."));

		assertThat(get("/api/playthroughs/99/journal")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/playthroughs/0/journal")).hasStatus(HttpStatus.NOT_FOUND);
	}

	private MockMvcTester.MockMvcRequestBuilder get(String uri) {
		return mvc.get().uri(uri).header(TestPlayerConfiguration.HEADER, "1");
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post()
			.uri("/api/playthroughs/5/journal")
			.header(TestPlayerConfiguration.HEADER, "1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json);
	}

}
