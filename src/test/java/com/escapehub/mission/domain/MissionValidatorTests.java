package com.escapehub.mission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MissionValidatorTests {

	private static final List<MissionPath> PATHS = List.of(new MissionPath("hall", "Διάδρομος"));

	@Test
	void acceptsValidMission() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found")),
				last(List.of("found")));

		assertThat(MissionValidator.validate(mission)).isEmpty();
	}

	@Test
	void acceptsValidPuzzleOfEveryType() {
		List<PuzzleAction> actions = List.of(new PuzzleAction("left", "Αριστερά"),
				new PuzzleAction("right", "Δεξιά"));
		Puzzle sequence = puzzle("sequence", PuzzleType.SEQUENCE, List.of(), List.of("a"), actions, null,
				List.of(), new Answer(null, List.of("right", "left"), null, null), false);
		Puzzle cooperative = puzzle("cooperative", PuzzleType.COOPERATIVE, List.of("a"), List.of("b"),
				List.of(), 2, List.of(), accepted("Mary Shelley"), false);
		Answer combinationAnswer = new Answer(null, null, List.of("a"), Map.of("year", List.of("1818")));
		Puzzle combination = puzzle("combination", PuzzleType.COMBINATION, List.of("b"), List.of(), List.of(),
				null, List.of(new PuzzleField("year", "Έτος")), combinationAnswer, true);
		Mission mission = mission(List.of(hidden("a"), hidden("b")), sequence, cooperative, combination);

		assertThat(MissionValidator.validate(mission)).isEmpty();
	}

	@Test
	void puzzleWithoutUnlockedByIsOpenFromTheStart() {
		Mission mission = mission(List.of(hidden("found")),
				text("first", List.of(), List.of("found")),
				last(List.of("found")));

		assertThat(MissionValidator.validate(mission)).isEmpty();
	}

	@Test
	void reportsUnknownEvidenceInUnlockedByAndReveals() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found", "lost")),
				last(List.of("found", "missing")));

		List<MissionProblem> problems = MissionValidator.validate(mission);

		assertProblem(problems, "puzzles[0].reveals[1]", "άγνωστο στοιχείο 'lost'");
		assertProblem(problems, "puzzles[1].unlockedBy[1]", "άγνωστο στοιχείο 'missing'");
	}

	@Test
	void reportsPuzzleUsedInsteadOfEvidence() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found")),
				last(List.of("first")));

		assertProblem(MissionValidator.validate(mission), "puzzles[1].unlockedBy[0]", "'first' είναι γρίφος");
	}

	@Test
	void reportsUnknownPath() {
		Mission mission = mission(List.of(new Evidence("start", "attic", "Ίχνος", true)),
				last(List.of("start")));

		assertProblem(MissionValidator.validate(mission), "evidence[0].path", "άγνωστη διαδρομή 'attic'");
	}

	@Test
	void reportsDuplicatePuzzleId() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found")),
				text("first", List.of("start"), List.of()),
				last(List.of("found")));

		assertProblem(MissionValidator.validate(mission), "puzzles[1].id", "διπλό id 'first'");
	}

	@Test
	void reportsEvidenceRevealedByTwoPuzzles() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found")),
				text("second", List.of("start"), List.of("found")),
				last(List.of("found")));

		assertProblem(MissionValidator.validate(mission), "puzzles[1].reveals[0]", "ήδη ο γρίφος 'first'");
	}

	@Test
	void reportsInitialEvidenceThatIsAlsoRevealed() {
		Mission mission = mission(List.of(initial("start")),
				text("first", List.of(), List.of("start")),
				last(List.of("start")));

		assertProblem(MissionValidator.validate(mission), "puzzles[0].reveals[0]", "διαθέσιμο από την αρχή");
	}

	@Test
	void reportsMissingFinalPuzzle() {
		Mission mission = mission(List.of(initial("start")), text("first", List.of("start"), List.of()));

		assertProblem(MissionValidator.validate(mission), "puzzles", "λείπει ο τελικός γρίφος");
	}

	@Test
	void reportsSecondFinalPuzzle() {
		Puzzle otherLast = puzzle("other-last", PuzzleType.TEXT_ANSWER, List.of("start"), List.of(), List.of(),
				null, List.of(), accepted("x"), true);
		Mission mission = mission(List.of(initial("start")), last(List.of("start")), otherLast);

		assertProblem(MissionValidator.validate(mission), "puzzles[1].completesMission", "ήδη τελικός γρίφος");
	}

	@Test
	void reportsCycleBetweenPuzzles() {
		Mission mission = mission(List.of(initial("start"), hidden("a-out"), hidden("b-out")),
				text("a", List.of("start", "b-out"), List.of("a-out")),
				text("b", List.of("a-out"), List.of("b-out")),
				last(List.of("start")));

		List<MissionProblem> problems = MissionValidator.validate(mission);

		assertProblem(problems, "puzzles[0].unlockedBy", "a → b → a");
		assertThat(problems).as("οι γρίφοι του κύκλου αναφέρονται μία φορά, στον κύκλο").hasSize(1);
	}

	@Test
	void reportsPuzzleThatWaitsForItsOwnEvidence() {
		Mission mission = mission(List.of(hidden("own")),
				text("selfish", List.of("own"), List.of("own")),
				last(List.of("own")));

		List<MissionProblem> problems = MissionValidator.validate(mission);

		assertProblem(problems, "puzzles[0].unlockedBy", "selfish → selfish");
		assertProblem(problems, "puzzles[1]", "ο τελικός γρίφος 'last' δεν ξεκλειδώνει ποτέ");
	}

	@Test
	void reportsFinalPuzzleThatNeverUnlocks() {
		Mission mission = mission(List.of(initial("start"), hidden("never")),
				last(List.of("start", "never")));

		List<MissionProblem> problems = MissionValidator.validate(mission);

		assertProblem(problems, "evidence[1]", "δεν αποκαλύπτεται ποτέ");
		assertProblem(problems, "puzzles[0]", "ο τελικός γρίφος 'last' δεν ξεκλειδώνει ποτέ");
	}

	@Test
	void reportsPuzzleThatNeverUnlocks() {
		Mission mission = mission(List.of(initial("start"), hidden("never")),
				text("stuck", List.of("never"), List.of()),
				last(List.of("start")));

		assertProblem(MissionValidator.validate(mission), "puzzles[0]", "γρίφος 'stuck' δεν ξεκλειδώνει");
	}

	@Test
	void checksUnlockGraphOnlyWhenReferencesAreValid() {
		Mission mission = mission(List.of(initial("start")), last(List.of("start", "typo")));

		assertThat(MissionValidator.validate(mission)).extracting(MissionProblem::field)
			.containsExactly("puzzles[0].unlockedBy[1]");
	}

	@Test
	void textAnswerNeedsAcceptedAnswer() {
		Puzzle puzzle = finalPuzzle(PuzzleType.TEXT_ANSWER, List.of(), null, List.of(), accepted());

		assertProblem(validate(puzzle), "puzzles[0].answer.accepted", "τουλάχιστον μία");
	}

	@Test
	void sequenceNeedsTwoActionsAndKnownSteps() {
		Puzzle puzzle = finalPuzzle(PuzzleType.SEQUENCE, List.of(new PuzzleAction("press", "Πάτα")), null,
				List.of(), new Answer(null, List.of("press", "pull"), null, null));

		List<MissionProblem> problems = validate(puzzle);

		assertProblem(problems, "puzzles[0].actions", "τουλάχιστον 2 ενέργειες");
		assertProblem(problems, "puzzles[0].answer.steps[1]", "άγνωστη ενέργεια 'pull'");
	}

	@Test
	void cooperativeNeedsAtLeastTwoContributors() {
		Puzzle puzzle = finalPuzzle(PuzzleType.COOPERATIVE, List.of(), 1, List.of(), accepted("x"));

		assertProblem(validate(puzzle), "puzzles[0].contributors", ">= 2");
	}

	@Test
	void cooperativeCannotNeedMorePlayersThanMinPlayers() {
		Puzzle puzzle = finalPuzzle(PuzzleType.COOPERATIVE, List.of(), 3, List.of(), accepted("x"));

		assertProblem(validate(puzzle), "puzzles[0].contributors", "minPlayers 2");
	}

	@Test
	void combinationNeedsAnAnswer() {
		Puzzle puzzle = finalPuzzle(PuzzleType.COMBINATION, List.of(), null, List.of(),
				new Answer(null, null, null, null));

		assertProblem(validate(puzzle), "puzzles[0].answer", "answer.evidence ή answer.values");
	}

	@Test
	void combinationNeedsKnownEvidenceAndOneValueListPerField() {
		List<PuzzleField> fields = List.of(new PuzzleField("place", "Τόπος"), new PuzzleField("year", "Έτος"));
		Answer answer = new Answer(null, null, List.of("ghost"),
				Map.of("place", List.of("Cologny"), "color", List.of("κόκκινο")));
		Puzzle puzzle = finalPuzzle(PuzzleType.COMBINATION, List.of(), null, fields, answer);

		List<MissionProblem> problems = validate(puzzle);

		assertProblem(problems, "puzzles[0].answer.evidence[0]", "άγνωστο στοιχείο 'ghost'");
		assertProblem(problems, "puzzles[0].answer.values.color", "άγνωστο πεδίο 'color'");
		assertProblem(problems, "puzzles[0].answer.values", "λείπει η απάντηση για το πεδίο 'year'");
	}

	@Test
	void rejectsFieldsThatBelongToAnotherType() {
		Puzzle puzzle = finalPuzzle(PuzzleType.TEXT_ANSWER, List.of(new PuzzleAction("a", "Α")), 2, List.of(),
				new Answer(List.of("x"), List.of("a"), null, null));

		List<MissionProblem> problems = validate(puzzle);

		assertProblem(problems, "puzzles[0].actions", "μόνο σε γρίφους [SEQUENCE]");
		assertProblem(problems, "puzzles[0].contributors", "μόνο σε γρίφους [COOPERATIVE]");
		assertProblem(problems, "puzzles[0].answer.steps", "μόνο σε γρίφους [SEQUENCE]");
	}

	@Test
	void initialEvidenceAndLookupsUseTheDefinition() {
		Mission mission = mission(List.of(initial("start"), hidden("found")),
				text("first", List.of("start"), List.of("found")),
				last(List.of("found")));

		assertThat(mission.initialEvidence()).extracting(Evidence::id).containsExactly("start");
		assertThat(mission.findEvidence("found")).map(Evidence::title).contains("Στοιχείο found");
		assertThat(mission.findPuzzle("last")).map(Puzzle::completesMission).contains(true);
		assertThat(mission.findPuzzle("nope")).isEmpty();
	}

	private static void assertProblem(List<MissionProblem> problems, String field, String messagePart) {
		assertThat(problems).anySatisfy(problem -> {
			assertThat(problem.field()).isEqualTo(field);
			assertThat(problem.message()).contains(messagePart);
		});
	}

	/**
	 * Αποστολή με έναν μόνο γρίφο, τον τελικό, που ανοίγει από το αρχικό στοιχείο {@code start}.
	 */
	private static List<MissionProblem> validate(Puzzle finalPuzzle) {
		return MissionValidator.validate(mission(List.of(initial("start")), finalPuzzle));
	}

	private static Mission mission(List<Evidence> evidence, Puzzle... puzzles) {
		return new Mission("test", 1, "Αποστολή", "Περιγραφή", 2, 10, PATHS, evidence, List.of(puzzles));
	}

	private static Evidence initial(String id) {
		return new Evidence(id, "hall", "Στοιχείο " + id, true);
	}

	private static Evidence hidden(String id) {
		return new Evidence(id, "hall", "Στοιχείο " + id, false);
	}

	private static Puzzle text(String id, List<String> unlockedBy, List<String> reveals) {
		return puzzle(id, PuzzleType.TEXT_ANSWER, unlockedBy, reveals, List.of(), null, List.of(),
				accepted("x"), false);
	}

	private static Puzzle last(List<String> unlockedBy) {
		return puzzle("last", PuzzleType.TEXT_ANSWER, unlockedBy, List.of(), List.of(), null, List.of(),
				accepted("x"), true);
	}

	private static Puzzle finalPuzzle(PuzzleType type, List<PuzzleAction> actions, Integer contributors,
			List<PuzzleField> fields, Answer answer) {
		return puzzle("last", type, List.of("start"), List.of(), actions, contributors, fields, answer, true);
	}

	private static Puzzle puzzle(String id, PuzzleType type, List<String> unlockedBy, List<String> reveals,
			List<PuzzleAction> actions, Integer contributors, List<PuzzleField> fields, Answer answer,
			boolean completesMission) {
		return new Puzzle(id, "hall", type, "Γρίφος " + id, null, unlockedBy, reveals, actions, contributors,
				fields, null, answer, completesMission);
	}

	private static Answer accepted(String... values) {
		return new Answer(List.of(values), null, null, null);
	}

}
