package com.escapehub.mission.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.Puzzle;

/**
 * Τα αρχεία αποστολών της εφαρμογής ({@code src/main/resources/missions}) και ο έλεγχος για διπλό id ανάμεσα σε
 * αρχεία (#26, AC2, AC4 και AC5).
 */
class MissionFilesTests {

	private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

	@AfterAll
	static void closeValidation() {
		VALIDATION.close();
	}

	/**
	 * Φορτώνει και επικυρώνει όλα τα αρχεία της εφαρμογής: ένα νέο άκυρο αρχείο κάνει αυτόν τον έλεγχο να
	 * αποτύχει πριν από το merge.
	 */
	@Test
	void everyMissionFileOfTheApplicationIsValid() {
		List<Mission> missions = files(MissionFiles.DEFAULT_LOCATION).loadAll();

		assertThat(missions).extracting(Mission::id).contains("villa-diodati");
	}

	@Test
	void developmentMissionCoversMvp3() {
		Mission mission = files(MissionFiles.DEFAULT_LOCATION).loadAll()
			.stream()
			.filter(candidate -> candidate.id().equals("villa-diodati"))
			.findFirst()
			.orElseThrow();

		assertThat(mission.paths()).hasSizeGreaterThanOrEqualTo(3);
		assertThat(mission.puzzles()).hasSizeGreaterThanOrEqualTo(4);
		Puzzle last = mission.puzzles().stream().filter(Puzzle::completesMission).findFirst().orElseThrow();
		assertThat(last.unlockedBy()).as("το τελικό βήμα θέλει ευρήματα από τις διαδρομές")
			.hasSizeGreaterThanOrEqualTo(3);
	}

	@Test
	void rejectsTwoFilesWithTheSameMissionId() {
		MissionFiles files = files("classpath:mission-fixtures/duplicate-mission-id/*.yml");

		assertThatThrownBy(files::loadAll).isInstanceOf(InvalidMissionException.class)
			.hasMessageContaining("second.yml")
			.hasMessageContaining("id: διπλό id 'same-id'")
			.hasMessageContaining("first.yml");
	}

	@Test
	void locationWithoutFilesGivesNoMissions() {
		assertThat(files("classpath*:no-such-folder/*.yml").loadAll()).isEmpty();
	}

	private static MissionFiles files(String location) {
		return new MissionFiles(location, new DefaultResourceLoader(), VALIDATION.getValidator());
	}

}
