package com.escapehub.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.escapehub.game.PlaythroughId;
import com.escapehub.team.TeamId;

class MissionStatusTests {

	private static final Instant START = Instant.parse("2026-11-26T10:00:00Z");

	@Test
	void withoutPlaythroughsMissionIsNotStarted() {
		assertThat(MissionStatus.of(List.of())).isEqualTo(MissionStatus.NOT_STARTED);
	}

	@Test
	void activePlaythroughMeansInProgress() {
		assertThat(MissionStatus.of(List.of(active(1)))).isEqualTo(MissionStatus.IN_PROGRESS);
	}

	@Test
	void onlyCompletedPlaythroughsMeanCompleted() {
		assertThat(MissionStatus.of(List.of(completed(1), completed(2)))).isEqualTo(MissionStatus.COMPLETED);
	}

	@Test
	void newActivePlaythroughAfterCompletedOneIsInProgress() {
		assertThat(MissionStatus.of(List.of(completed(1), active(2)))).isEqualTo(MissionStatus.IN_PROGRESS);
	}

	@Test
	void statusOfSinglePlaythrough() {
		assertThat(MissionStatus.of(active(1))).isEqualTo(MissionStatus.IN_PROGRESS);
		assertThat(MissionStatus.of(completed(1))).isEqualTo(MissionStatus.COMPLETED);
	}

	@Test
	void labelsAreGreek() {
		assertThat(MissionStatus.NOT_STARTED.label()).isEqualTo("Δεν έχει ξεκινήσει");
		assertThat(MissionStatus.IN_PROGRESS.label()).isEqualTo("Σε εξέλιξη");
		assertThat(MissionStatus.COMPLETED.label()).isEqualTo("Ολοκληρώθηκε");
	}

	private static Playthrough active(long id) {
		return new Playthrough(new PlaythroughId(id), new TeamId(10), "villa-diodati", 1, START, null);
	}

	private static Playthrough completed(long id) {
		return new Playthrough(new PlaythroughId(id), new TeamId(10), "villa-diodati", 1, START,
				START.plusSeconds(3600));
	}

}
