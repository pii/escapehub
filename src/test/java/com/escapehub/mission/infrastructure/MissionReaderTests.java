package com.escapehub.mission.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.escapehub.mission.domain.Evidence;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.domain.Puzzle;
import com.escapehub.mission.domain.PuzzleType;

/**
 * Ανάγνωση και επικύρωση ενός αρχείου αποστολής, με έγκυρα και άκυρα fixtures από το
 * {@code src/test/resources/mission-fixtures} (#26, AC2 και AC5).
 */
class MissionReaderTests {

	private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

	private final MissionReader reader = new MissionReader(VALIDATION.getValidator());

	@AfterAll
	static void closeValidation() {
		VALIDATION.close();
	}

	@Test
	void readsValidMissionWithEveryPuzzleType() throws IOException {
		Mission mission = read("mission-fixtures/valid/small.yml");

		assertThat(mission.id()).isEqualTo("small-test");
		assertThat(mission.version()).isEqualTo(3);
		assertThat(mission.paths()).hasSize(2);
		assertThat(mission.initialEvidence()).extracting(Evidence::id).containsExactly("note");
		assertThat(mission.puzzles()).extracting(Puzzle::type)
			.containsExactly(PuzzleType.TEXT_ANSWER, PuzzleType.SEQUENCE, PuzzleType.COOPERATIVE,
					PuzzleType.COMBINATION);
		Puzzle sequence = mission.findPuzzle("open-box").orElseThrow();
		assertThat(sequence.unlockedBy()).as("χωρίς unlockedBy: ανοιχτός από την αρχή").isEmpty();
		assertThat(sequence.answer().steps()).containsExactly("right", "left", "right");
		assertThat(mission.findPuzzle("final-door").orElseThrow().answer().values())
			.containsEntry("word", List.of("secret-word"));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			unknown-evidence.yml    | puzzles[1].unlockedBy[1] | άγνωστο στοιχείο 'missing-clue'
			puzzle-as-evidence.yml  | puzzles[1].unlockedBy[0] | το 'first' είναι γρίφος
			unknown-puzzle-type.yml | puzzles[0].type          | άγνωστη τιμή 'RIDDLE'
			duplicate-id.yml        | evidence[2].id           | διπλό id 'clue'
			unlock-cycle.yml        | puzzles[0].unlockedBy    | first → second → first
			final-never-unlocks.yml | puzzles[1]               | τελικός γρίφος 'last' δεν ξεκλειδώνει
			unknown-field.yml       | puzzles[0].unlockBy      | άγνωστο πεδίο 'unlockBy'
			missing-title.yml       | puzzles[0].title         | ''
			""")
	void rejectsInvalidFileNamingFileAndField(String file, String field, String messagePart) {
		assertThatThrownBy(() -> read("mission-fixtures/invalid/" + file))
			.isInstanceOfSatisfying(InvalidMissionException.class, ex -> {
				assertThat(ex.getMessage()).contains(file).contains(field + ": ");
				assertThat(ex.problems()).anySatisfy(problem -> {
					assertThat(problem.field()).isEqualTo(field);
					assertThat(problem.message()).contains(messagePart);
				});
			});
	}

	@Test
	void unknownPuzzleTypeListsTheKnownTypes() {
		assertThatThrownBy(() -> read("mission-fixtures/invalid/unknown-puzzle-type.yml"))
			.hasMessageContaining("TEXT_ANSWER, SEQUENCE, COOPERATIVE, COMBINATION")
			.hasMessageContaining("γραμμή 16");
	}

	@Test
	void rejectsDuplicateKeys() {
		String yaml = """
				id: one
				id: two
				""";

		assertThatThrownBy(() -> read(yaml, "duplicate-keys.yml")).isInstanceOf(InvalidMissionException.class)
			.hasMessageContaining("duplicate-keys.yml")
			.hasMessageContaining("id");
	}

	@Test
	void rejectsInvalidIdFormat() {
		String yaml = """
				id: Villa Diodati
				""";

		assertThatThrownBy(() -> read(yaml, "bad-id.yml")).isInstanceOf(InvalidMissionException.class)
			.hasMessageContaining("id: επιτρέπονται μόνο πεζά λατινικά");
	}

	@Test
	void rejectsEmptyFile() {
		assertThatThrownBy(() -> read("", "empty.yml")).isInstanceOf(InvalidMissionException.class)
			.hasMessageContaining("empty.yml");
	}

	private Mission read(String classpathLocation) throws IOException {
		Resource resource = new ClassPathResource(classpathLocation);
		try (InputStream content = resource.getInputStream()) {
			return reader.read(resource.getDescription(), content);
		}
	}

	private Mission read(String yaml, String source) {
		return reader.read(source, new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
	}

}
