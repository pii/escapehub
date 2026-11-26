package com.escapehub.mission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;

import com.escapehub.mission.domain.Answer;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.Puzzle;
import com.escapehub.mission.infrastructure.MissionFiles;

/**
 * Οι σωστές απαντήσεις δεν φεύγουν από το backend (#26, AC3, ADR-0001). Ελέγχεται για όλα τα αρχεία της
 * εφαρμογής και για το fixture με έναν γρίφο από κάθε τύπο:
 * <ul>
 * <li>σε JSON (όπως στις αποκρίσεις του REST API) δεν υπάρχει το {@code answer} ούτε οι αποδεκτές τιμές,</li>
 * <li>το {@code toString()} (π.χ. σε log ή σε template) δεν τις δείχνει.</li>
 * </ul>
 * Τα βήματα ενός SEQUENCE και τα στοιχεία ενός COMBINATION είναι id που δημοσιεύονται έτσι κι αλλιώς (ενέργειες,
 * ημερολόγιο). Μυστική είναι η σειρά και η επιλογή τους, που υπάρχουν μόνο μέσα στο {@code answer}. Για τη σειρά
 * ελέγχεται και η λίστα των βημάτων.
 */
class MissionAnswersAreHiddenTests {

	private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@AfterAll
	static void closeValidation() {
		VALIDATION.close();
	}

	static Stream<Named<Mission>> missions() {
		return Stream.of(MissionFiles.DEFAULT_LOCATION, "classpath:mission-fixtures/valid/*.yml")
			.flatMap(location -> files(location).loadAll().stream())
			.map(mission -> Named.of(mission.id(), mission));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("missions")
	void answersAreReadFromTheFile(Mission mission) {
		assertThat(mission.puzzles()).allSatisfy(puzzle -> assertThat(puzzle.answer()).isNotNull());
		assertThat(mission.puzzles()).flatMap(MissionAnswersAreHiddenTests::secrets).isNotEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("missions")
	void answersAreNotWrittenToJson(Mission mission) {
		String json = JSON.writeValueAsString(mission);

		assertThat(json).contains("\"puzzles\"").doesNotContain("\"answer\"");
		for (Puzzle puzzle : mission.puzzles()) {
			assertThat(JSON.writeValueAsString(puzzle)).as(puzzle.id()).doesNotContain(secrets(puzzle));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("missions")
	void toStringDoesNotShowAnswers(Mission mission) {
		for (Puzzle puzzle : mission.puzzles()) {
			assertThat(puzzle.toString()).as(puzzle.id())
				.contains("Answer[κρυφή]")
				.doesNotContain(secrets(puzzle));
		}
	}

	private static MissionFiles files(String location) {
		return new MissionFiles(location, new DefaultResourceLoader(), VALIDATION.getValidator());
	}

	/**
	 * Οι αποδεκτές απαντήσεις, οι αποδεκτές τιμές των πεδίων και η σωστή σειρά των βημάτων, όπως θα εμφανιζόταν
	 * σε JSON και σε {@code toString()}.
	 */
	private static List<String> secrets(Puzzle puzzle) {
		Answer answer = puzzle.answer();
		List<String> secrets = new ArrayList<>(answer.accepted());
		answer.values().values().forEach(secrets::addAll);
		if (!answer.steps().isEmpty()) {
			secrets.add(JSON.writeValueAsString(answer.steps()));
			secrets.add(answer.steps().toString());
		}
		return secrets;
	}

}
