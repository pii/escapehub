package com.escapehub.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.escapehub.mission.domain.Evidence;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.MissionPath;

class EvidenceAvailabilityTests {

	private static final Mission MISSION = new Mission("test", 1, "Αποστολή", "Περίληψη", 1, 10,
			List.of(new MissionPath("hall", "Διάδρομος")),
			List.of(new Evidence("note", "hall", "Σημείωμα", true),
					new Evidence("key", "hall", "Κλειδί", false),
					new Evidence("map", "hall", "Χάρτης", true)),
			List.of());

	@Test
	void initialEvidenceIsAvailableInFileOrder() {
		assertThat(EvidenceAvailability.available(MISSION)).extracting(Evidence::id)
			.containsExactly("note", "map");
	}

	@Test
	void initialEvidenceIsAvailable() {
		assertThat(EvidenceAvailability.isAvailable(MISSION, "note")).isTrue();
	}

	@Test
	void evidenceRevealedByPuzzleIsNotAvailableYet() {
		assertThat(EvidenceAvailability.isAvailable(MISSION, "key")).isFalse();
	}

	@Test
	void unknownEvidenceIsNotAvailable() {
		assertThat(EvidenceAvailability.isAvailable(MISSION, "ghost")).isFalse();
		assertThat(EvidenceAvailability.isAvailable(MISSION, null)).isFalse();
	}

}
