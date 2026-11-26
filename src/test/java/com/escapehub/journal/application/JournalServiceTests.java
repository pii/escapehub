package com.escapehub.journal.application;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.escapehub.game.PlaythroughId;
import com.escapehub.journal.infrastructure.JournalEntryEntity;
import com.escapehub.journal.infrastructure.JournalEntryRepository;
import com.escapehub.user.PlayerId;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalServiceTests {

	private static final PlaythroughId PLAYTHROUGH = new PlaythroughId(5);

	private static final Instant TIME = Instant.parse("2026-11-26T14:00:00Z");

	@Mock
	private JournalEntryRepository repository;

	@Test
	void recordReportsWhetherTheEntryIsNew() {
		when(repository.saveIfAbsent(5, "note", 1)).thenReturn(1);
		when(repository.saveIfAbsent(5, "note", 2)).thenReturn(0);
		JournalService journal = new JournalService(repository);

		assertThat(journal.record(PLAYTHROUGH, "note", new PlayerId(1))).isTrue();
		assertThat(journal.record(PLAYTHROUGH, "note", new PlayerId(2))).isFalse();
	}

	@Test
	void entriesKeepTheOrderOfTheRepository() {
		when(repository.findByPlaythroughIdOrderByAddedAtDescIdDesc(5L))
			.thenReturn(List.of(entity("map", 2, TIME.plusSeconds(60)), entity("note", 1, TIME)));

		List<JournalEntry> entries = new JournalService(repository).entries(PLAYTHROUGH);

		assertThat(entries).containsExactly(new JournalEntry("map", new PlayerId(2), TIME.plusSeconds(60)),
				new JournalEntry("note", new PlayerId(1), TIME));
	}

	@Test
	void entryOfEvidence() {
		when(repository.findByPlaythroughIdAndEvidenceId(5L, "note"))
			.thenReturn(Optional.of(entity("note", 1, TIME)));

		assertThat(new JournalService(repository).entry(PLAYTHROUGH, "note"))
			.contains(new JournalEntry("note", new PlayerId(1), TIME));
		assertThat(new JournalService(repository).entry(PLAYTHROUGH, "map")).isEmpty();
	}

	private static JournalEntryEntity entity(String evidenceId, long player, Instant addedAt) {
		JournalEntryEntity entity = BeanUtils.instantiateClass(JournalEntryEntity.class);
		ReflectionTestUtils.setField(entity, "playthroughId", 5L);
		ReflectionTestUtils.setField(entity, "evidenceId", evidenceId);
		ReflectionTestUtils.setField(entity, "addedBy", player);
		ReflectionTestUtils.setField(entity, "addedAt", addedAt);
		return entity;
	}

}
