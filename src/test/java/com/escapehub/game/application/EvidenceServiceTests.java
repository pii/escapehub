package com.escapehub.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.escapehub.common.ConflictException;
import com.escapehub.common.ForbiddenException;
import com.escapehub.game.PlaythroughId;
import com.escapehub.game.domain.MissionStatus;
import com.escapehub.journal.application.JournalEntry;
import com.escapehub.journal.application.JournalService;
import com.escapehub.mission.application.MissionCatalog;
import com.escapehub.mission.domain.Evidence;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionPath;
import com.escapehub.team.TeamId;
import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvidenceServiceTests {

	private static final PlayerId MARIA = new PlayerId(1);

	private static final PlayerId NIKOS = new PlayerId(2);

	private static final PlaythroughId PLAYTHROUGH = new PlaythroughId(5);

	private static final Instant TIME = Instant.parse("2026-11-26T14:00:00Z");

	private static final Mission MISSION = new Mission("villa-diodati", 1, "Το μυστήριο", "Περίληψη", 2, 45,
			List.of(new MissionPath("letter", "Η επιστολή"), new MissionPath("portrait", "Το πορτρέτο")),
			List.of(new Evidence("sealed-letter", "letter", "Σφραγισμένη επιστολή", true),
					new Evidence("portrait-back", "portrait", "Επιγραφή του πορτρέτου", true),
					new Evidence("letter-1816", "letter", "Επιστολή του 1816", false)),
			List.of());

	@Mock
	private PlaythroughService playthroughs;

	@Mock
	private MissionCatalog missions;

	@Mock
	private JournalService journal;

	@Mock
	private PlayerService players;

	@Test
	void examiningAvailableObjectAddsEvidenceWithPlayerAndTime() {
		givenPlaythrough(MARIA, null);
		when(journal.record(PLAYTHROUGH, "sealed-letter", MARIA)).thenReturn(true);
		when(journal.entry(PLAYTHROUGH, "sealed-letter"))
			.thenReturn(Optional.of(new JournalEntry("sealed-letter", MARIA, TIME)));

		ExamineResult result = service().examine(MARIA, PLAYTHROUGH, "sealed-letter");

		assertThat(result.added()).isTrue();
		assertThat(result.item())
			.isEqualTo(new JournalItem("sealed-letter", "Σφραγισμένη επιστολή", MARIA, "Μαρία", TIME));
	}

	@Test
	void examiningEvidenceAlreadyInJournalIsNotAnError() {
		givenPlaythrough(NIKOS, null);
		when(journal.record(PLAYTHROUGH, "sealed-letter", NIKOS)).thenReturn(false);
		when(journal.entry(PLAYTHROUGH, "sealed-letter"))
			.thenReturn(Optional.of(new JournalEntry("sealed-letter", MARIA, TIME)));

		ExamineResult result = service().examine(NIKOS, PLAYTHROUGH, "sealed-letter");

		assertThat(result.added()).isFalse();
		assertThat(result.item().addedByName()).as("μένει αυτός που το βρήκε πρώτος").isEqualTo("Μαρία");
	}

	@Test
	void evidenceNotRevealedYetIsConflictAndJournalIsUnchanged() {
		givenPlaythrough(MARIA, null);

		assertThatThrownBy(() -> service().examine(MARIA, PLAYTHROUGH, "letter-1816"))
			.isInstanceOf(ConflictException.class)
			.hasMessage(EvidenceService.NOT_AVAILABLE);
		verify(journal, never()).record(any(), anyString(), any());
	}

	@Test
	void unknownEvidenceIsConflict() {
		givenPlaythrough(MARIA, null);

		assertThatThrownBy(() -> service().examine(MARIA, PLAYTHROUGH, "ghost"))
			.isInstanceOf(ConflictException.class)
			.hasMessage(EvidenceService.NOT_AVAILABLE);
		verify(journal, never()).record(any(), anyString(), any());
	}

	@Test
	void completedPlaythroughJournalDoesNotChange() {
		givenPlaythrough(MARIA, TIME.plusSeconds(3600));

		assertThatThrownBy(() -> service().examine(MARIA, PLAYTHROUGH, "sealed-letter"))
			.isInstanceOf(ConflictException.class)
			.hasMessage(EvidenceService.COMPLETED);
		verify(journal, never()).record(any(), anyString(), any());
	}

	@Test
	void outsiderCannotSeeOrChangeJournal() {
		when(playthroughs.details(NIKOS, PLAYTHROUGH)).thenThrow(new ForbiddenException("Άλλη ομάδα."));

		assertThatThrownBy(() -> service().examine(NIKOS, PLAYTHROUGH, "sealed-letter"))
			.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> service().journal(NIKOS, PLAYTHROUGH)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> service().examinable(NIKOS, PLAYTHROUGH))
			.isInstanceOf(ForbiddenException.class);
		verify(journal, never()).record(any(), anyString(), any());
	}

	@Test
	void journalShowsEntriesNewestFirstWithTitleAndPlayerName() {
		givenPlaythrough(MARIA, null);
		when(journal.entries(PLAYTHROUGH)).thenReturn(List.of(
				new JournalEntry("portrait-back", NIKOS, TIME.plusSeconds(60)),
				new JournalEntry("sealed-letter", MARIA, TIME)));

		Instant later = TIME.plusSeconds(60);
		assertThat(service().journal(MARIA, PLAYTHROUGH)).containsExactly(
				new JournalItem("portrait-back", "Επιγραφή του πορτρέτου", NIKOS, "Νίκος", later),
				new JournalItem("sealed-letter", "Σφραγισμένη επιστολή", MARIA, "Μαρία", TIME));
	}

	@Test
	void examinableObjectsAreTheAvailableOnesMarkedWhenInJournal() {
		givenPlaythrough(MARIA, null);
		when(journal.entries(PLAYTHROUGH)).thenReturn(List.of(new JournalEntry("sealed-letter", MARIA, TIME)));

		assertThat(service().examinable(MARIA, PLAYTHROUGH)).containsExactly(
				new ExaminableEvidence("sealed-letter", "Σφραγισμένη επιστολή", "Η επιστολή", true),
				new ExaminableEvidence("portrait-back", "Επιγραφή του πορτρέτου", "Το πορτρέτο",
						false));
	}

	private EvidenceService service() {
		return new EvidenceService(playthroughs, missions, journal, players);
	}

	private void givenPlaythrough(PlayerId viewer, Instant completedAt) {
		MissionStatus status = (completedAt == null) ? MissionStatus.IN_PROGRESS : MissionStatus.COMPLETED;
		PlaythroughDetails details = new PlaythroughDetails(PLAYTHROUGH, new TeamId(10), "Οι Ερευνητές",
				"villa-diodati", 1, "Το μυστήριο", MISSION.paths(), TIME, completedAt, status);
		when(playthroughs.details(viewer, PLAYTHROUGH)).thenReturn(details);
		when(missions.find("villa-diodati")).thenReturn(Optional.of(MISSION));
		when(players.findName(MARIA)).thenReturn(Optional.of("Μαρία"));
		when(players.findName(NIKOS)).thenReturn(Optional.of("Νίκος"));
	}

}
