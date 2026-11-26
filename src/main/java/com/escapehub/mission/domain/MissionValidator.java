package com.escapehub.mission.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Οι έλεγχοι μιας αποστολής που δεν εκφράζονται με Bean Validation (ADR-0001):
 * <ul>
 * <li>μοναδικά id, αναφορές μόνο σε διαδρομές, στοιχεία και ενέργειες που υπάρχουν,</li>
 * <li>τα πεδία που χρειάζεται κάθε τύπος γρίφου,</li>
 * <li>ακριβώς ένας τελικός γρίφος, και κάθε στοιχείο το αποκαλύπτει το πολύ ένας γρίφος,</li>
 * <li>χωρίς κύκλους στους κανόνες ξεκλειδώματος, και η αποστολή ολοκληρώνεται.</li>
 * </ul>
 * Ο γράφος ξεκλειδώματος ελέγχεται μόνο αν οι αναφορές είναι σωστές, για να μη βγαίνουν δευτερεύοντα λάθη από το
 * ίδιο πρόβλημα.
 *
 * <p>Προϋπόθεση: η αποστολή έχει περάσει το Bean Validation, άρα τα υποχρεωτικά πεδία δεν είναι {@code null}.
 */
public final class MissionValidator {

	private final Mission mission;

	private final List<MissionProblem> problems = new ArrayList<>();

	private final Set<String> pathIds;

	private final Map<String, Evidence> evidenceById;

	private final Map<String, Puzzle> puzzlesById;

	/** Ποιος γρίφος αποκαλύπτει κάθε στοιχείο. */
	private final Map<String, Puzzle> revealedBy = new HashMap<>();

	private MissionValidator(Mission mission) {
		this.mission = mission;
		this.pathIds = index("paths", mission.paths(), MissionPath::id).keySet();
		this.evidenceById = index("evidence", mission.evidence(), Evidence::id);
		this.puzzlesById = index("puzzles", mission.puzzles(), Puzzle::id);
	}

	/**
	 * Όλα τα προβλήματα της αποστολής, ή κενή λίστα αν είναι έγκυρη.
	 */
	public static List<MissionProblem> validate(Mission mission) {
		return new MissionValidator(mission).run();
	}

	private List<MissionProblem> run() {
		List<Evidence> evidence = mission.evidence();
		for (int i = 0; i < evidence.size(); i++) {
			knownPath("evidence[" + i + "].path", evidence.get(i).path());
		}
		List<Puzzle> puzzles = mission.puzzles();
		for (int i = 0; i < puzzles.size(); i++) {
			checkPuzzle("puzzles[" + i + "]", puzzles.get(i));
		}
		checkFinalPuzzle();
		if (problems.isEmpty()) {
			checkUnlockGraph();
		}
		return List.copyOf(problems);
	}

	private void checkPuzzle(String field, Puzzle puzzle) {
		if (puzzle.path() != null) {
			knownPath(field + ".path", puzzle.path());
		}
		for (int j = 0; j < puzzle.unlockedBy().size(); j++) {
			knownEvidence(field + ".unlockedBy[" + j + "]", puzzle.unlockedBy().get(j));
		}
		for (int j = 0; j < puzzle.reveals().size(); j++) {
			checkReveal(field + ".reveals[" + j + "]", puzzle, puzzle.reveals().get(j));
		}
		checkFieldsOfType(field, puzzle);
		switch (puzzle.type()) {
			case TEXT_ANSWER -> atLeastOne(field + ".answer.accepted", puzzle.answer().accepted());
			case SEQUENCE -> checkSequence(field, puzzle);
			case COOPERATIVE -> checkCooperative(field, puzzle);
			case COMBINATION -> checkCombination(field, puzzle);
			default -> throw new IllegalStateException("Άγνωστος τύπος γρίφου: " + puzzle.type());
		}
	}

	private void checkReveal(String field, Puzzle puzzle, String evidenceId) {
		if (!knownEvidence(field, evidenceId)) {
			return;
		}
		if (evidenceById.get(evidenceId).initial()) {
			problem(field, "το στοιχείο '" + evidenceId + "' είναι ήδη διαθέσιμο από την αρχή (initial)");
		}
		Puzzle previous = revealedBy.putIfAbsent(evidenceId, puzzle);
		if (previous != null && previous != puzzle) {
			problem(field, "το στοιχείο '" + evidenceId + "' το αποκαλύπτει ήδη ο γρίφος '" + previous.id()
					+ "'");
		}
	}

	/**
	 * Κάθε πεδίο που αφορά συγκεκριμένους τύπους επιτρέπεται μόνο σε αυτούς, ώστε ένα λάθος να μη χάνεται
	 * σιωπηλά (π.χ. {@code contributors} σε γρίφο {@code TEXT_ANSWER}).
	 */
	private void checkFieldsOfType(String field, Puzzle puzzle) {
		Answer answer = puzzle.answer();
		onlyFor(field + ".actions", puzzle, !puzzle.actions().isEmpty(), PuzzleType.SEQUENCE);
		onlyFor(field + ".contributors", puzzle, puzzle.contributors() != null, PuzzleType.COOPERATIVE);
		onlyFor(field + ".fields", puzzle, !puzzle.fields().isEmpty(), PuzzleType.COMBINATION);
		onlyFor(field + ".answer.accepted", puzzle, !answer.accepted().isEmpty(), PuzzleType.TEXT_ANSWER,
				PuzzleType.COOPERATIVE);
		onlyFor(field + ".answer.steps", puzzle, !answer.steps().isEmpty(), PuzzleType.SEQUENCE);
		onlyFor(field + ".answer.evidence", puzzle, !answer.evidence().isEmpty(), PuzzleType.COMBINATION);
		onlyFor(field + ".answer.values", puzzle, !answer.values().isEmpty(), PuzzleType.COMBINATION);
	}

	private void checkSequence(String field, Puzzle puzzle) {
		if (puzzle.actions().size() < 2) {
			problem(field + ".actions", "ένας γρίφος SEQUENCE χρειάζεται τουλάχιστον 2 ενέργειες");
		}
		Set<String> actionIds = index(field + ".actions", puzzle.actions(), PuzzleAction::id).keySet();
		List<String> steps = puzzle.answer().steps();
		if (steps.size() < 2) {
			problem(field + ".answer.steps", "η σωστή σειρά χρειάζεται τουλάχιστον 2 βήματα");
		}
		for (int j = 0; j < steps.size(); j++) {
			if (!actionIds.contains(steps.get(j))) {
				problem(field + ".answer.steps[" + j + "]", "άγνωστη ενέργεια '" + steps.get(j) + "'");
			}
		}
	}

	private void checkCooperative(String field, Puzzle puzzle) {
		Integer contributors = puzzle.contributors();
		if (contributors == null || contributors < 2) {
			problem(field + ".contributors", "ένας γρίφος COOPERATIVE χρειάζεται contributors >= 2");
		}
		else if (contributors > mission.minPlayers()) {
			problem(field + ".contributors", "ο γρίφος θέλει " + contributors + " παίκτες, αλλά η αποστολή"
					+ " δηλώνει minPlayers " + mission.minPlayers());
		}
		atLeastOne(field + ".answer.accepted", puzzle.answer().accepted());
	}

	private void checkCombination(String field, Puzzle puzzle) {
		Answer answer = puzzle.answer();
		Set<String> fieldIds = index(field + ".fields", puzzle.fields(), PuzzleField::id).keySet();
		if (answer.evidence().isEmpty() && answer.values().isEmpty()) {
			problem(field + ".answer", "ένας γρίφος COMBINATION θέλει answer.evidence ή answer.values");
		}
		for (int j = 0; j < answer.evidence().size(); j++) {
			knownEvidence(field + ".answer.evidence[" + j + "]", answer.evidence().get(j));
		}
		answer.values().forEach((key, values) -> {
			if (fieldIds.contains(key)) {
				atLeastOne(field + ".answer.values." + key, values);
			}
			else {
				problem(field + ".answer.values." + key, "άγνωστο πεδίο '" + key + "'");
			}
		});
		for (String fieldId : fieldIds) {
			if (!answer.values().containsKey(fieldId)) {
				problem(field + ".answer.values", "λείπει η απάντηση για το πεδίο '" + fieldId + "'");
			}
		}
	}

	private void checkFinalPuzzle() {
		List<Puzzle> puzzles = mission.puzzles();
		Puzzle first = null;
		for (int i = 0; i < puzzles.size(); i++) {
			if (!puzzles.get(i).completesMission()) {
				continue;
			}
			if (first == null) {
				first = puzzles.get(i);
			}
			else {
				String field = "puzzles[" + i + "].completesMission";
				problem(field, "υπάρχει ήδη τελικός γρίφος, ο '" + first.id() + "'");
			}
		}
		if (first == null) {
			problem("puzzles", "λείπει ο τελικός γρίφος: ένας γρίφος πρέπει να έχει completesMission true");
		}
	}

	private void checkUnlockGraph() {
		List<Evidence> evidence = mission.evidence();
		for (int i = 0; i < evidence.size(); i++) {
			Evidence item = evidence.get(i);
			if (!item.initial() && !revealedBy.containsKey(item.id())) {
				problem("evidence[" + i + "]", "το στοιχείο '" + item.id() + "' δεν αποκαλύπτεται ποτέ:"
						+ " δεν είναι initial και κανένας γρίφος δεν το έχει στο reveals");
			}
		}
		Set<String> inCycle = findCycles();
		Set<String> unlocked = unlockedPuzzles();
		List<Puzzle> puzzles = mission.puzzles();
		for (int i = 0; i < puzzles.size(); i++) {
			Puzzle puzzle = puzzles.get(i);
			if (unlocked.contains(puzzle.id())) {
				continue;
			}
			String field = "puzzles[" + i + "]";
			if (puzzle.completesMission()) {
				problem(field, "ο τελικός γρίφος '" + puzzle.id() + "' δεν ξεκλειδώνει ποτέ, οπότε η"
						+ " αποστολή δεν ολοκληρώνεται");
			}
			else if (!inCycle.contains(puzzle.id())) {
				problem(field, "ο γρίφος '" + puzzle.id() + "' δεν ξεκλειδώνει ποτέ");
			}
		}
	}

	/**
	 * Παίζει την αποστολή: ξεκινά με τα αρχικά στοιχεία και λύνει κάθε γρίφο που ανοίγει, μέχρι να μην ανοίγει
	 * κανένας άλλος. Επιστρέφει τους γρίφους που άνοιξαν.
	 */
	private Set<String> unlockedPuzzles() {
		Set<String> available = mission.initialEvidence()
			.stream()
			.map(Evidence::id)
			.collect(Collectors.toCollection(HashSet::new));
		Set<String> unlocked = new HashSet<>();
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Puzzle puzzle : mission.puzzles()) {
				if (!unlocked.contains(puzzle.id()) && available.containsAll(puzzle.unlockedBy())) {
					unlocked.add(puzzle.id());
					available.addAll(puzzle.reveals());
					changed = true;
				}
			}
		}
		return unlocked;
	}

	/**
	 * Αναζήτηση κύκλων (DFS): ο γρίφος Α περιμένει στοιχείο που αποκαλύπτει ο Β, και ο Β, ίσως μέσω άλλων,
	 * περιμένει στοιχείο που αποκαλύπτει ο Α. Επιστρέφει τους γρίφους που ανήκουν σε κύκλο.
	 */
	private Set<String> findCycles() {
		Set<String> inCycle = new HashSet<>();
		Set<String> done = new HashSet<>();
		List<String> trail = new ArrayList<>();
		mission.puzzles().forEach(puzzle -> visit(puzzle.id(), trail, done, inCycle));
		return inCycle;
	}

	private void visit(String puzzleId, List<String> trail, Set<String> done, Set<String> inCycle) {
		if (done.contains(puzzleId)) {
			return;
		}
		int start = trail.indexOf(puzzleId);
		if (start >= 0) {
			List<String> cycle = new ArrayList<>(trail.subList(start, trail.size()));
			inCycle.addAll(cycle);
			cycle.add(puzzleId);
			problem(puzzleField(cycle.get(0)) + ".unlockedBy", "κύκλος στους κανόνες ξεκλειδώματος (κάθε"
					+ " γρίφος περιμένει στοιχείο από τον επόμενο): " + String.join(" → ", cycle));
			return;
		}
		trail.add(puzzleId);
		for (String evidenceId : puzzlesById.get(puzzleId).unlockedBy()) {
			Puzzle revealer = revealedBy.get(evidenceId);
			if (revealer != null) {
				visit(revealer.id(), trail, done, inCycle);
			}
		}
		trail.remove(trail.size() - 1);
		done.add(puzzleId);
	}

	private String puzzleField(String puzzleId) {
		return "puzzles[" + mission.puzzles().indexOf(puzzlesById.get(puzzleId)) + "]";
	}

	private void knownPath(String field, String pathId) {
		if (!pathIds.contains(pathId)) {
			problem(field, "άγνωστη διαδρομή '" + pathId + "'");
		}
	}

	private boolean knownEvidence(String field, String evidenceId) {
		if (evidenceById.containsKey(evidenceId)) {
			return true;
		}
		if (puzzlesById.containsKey(evidenceId)) {
			problem(field, "το '" + evidenceId + "' είναι γρίφος, ενώ εδώ χρειάζεται στοιχείο (evidence)");
		}
		else {
			problem(field, "άγνωστο στοιχείο '" + evidenceId + "'");
		}
		return false;
	}

	private void onlyFor(String field, Puzzle puzzle, boolean present, PuzzleType... types) {
		if (present && !Arrays.asList(types).contains(puzzle.type())) {
			problem(field, "επιτρέπεται μόνο σε γρίφους " + Arrays.toString(types) + ", ενώ ο '"
					+ puzzle.id() + "' είναι " + puzzle.type());
		}
	}

	private void atLeastOne(String field, List<String> values) {
		if (values.isEmpty()) {
			problem(field, "χρειάζεται τουλάχιστον μία αποδεκτή τιμή");
		}
	}

	/**
	 * Τα αντικείμενα ανά id. Για κάθε διπλό id καταγράφει πρόβλημα και κρατά το πρώτο.
	 */
	private <T> Map<String, T> index(String field, List<T> items, Function<T, String> idOf) {
		Map<String, T> byId = new LinkedHashMap<>();
		Map<String, Integer> firstIndex = new HashMap<>();
		for (int i = 0; i < items.size(); i++) {
			String id = idOf.apply(items.get(i));
			Integer first = firstIndex.putIfAbsent(id, i);
			if (first == null) {
				byId.put(id, items.get(i));
			}
			else {
				String existing = field + "[" + first + "]";
				problem(field + "[" + i + "].id", "διπλό id '" + id + "', υπάρχει ήδη στο " + existing);
			}
		}
		return byId;
	}

	private void problem(String field, String message) {
		problems.add(new MissionProblem(field, message));
	}

}
