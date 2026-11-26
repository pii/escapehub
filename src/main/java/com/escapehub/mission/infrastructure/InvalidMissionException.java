package com.escapehub.mission.infrastructure;

import java.util.List;
import java.util.stream.Collectors;

import com.escapehub.mission.domain.MissionProblem;

/**
 * Ένα αρχείο αποστολής είναι άκυρο. Σταματά την εκκίνηση της εφαρμογής, με μήνυμα που δείχνει το αρχείο και κάθε
 * πεδίο με πρόβλημα.
 */
public class InvalidMissionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String source;

	private final List<MissionProblem> problems;

	public InvalidMissionException(String source, List<MissionProblem> problems) {
		super(message(source, problems));
		this.source = source;
		this.problems = List.copyOf(problems);
	}

	/**
	 * Το αρχείο, όπως το περιγράφει το Spring, π.χ. {@code file [.../missions/villa-diodati.yml]}.
	 */
	public String source() {
		return source;
	}

	public List<MissionProblem> problems() {
		return problems;
	}

	private static String message(String source, List<MissionProblem> problems) {
		return problems.stream()
			.map(problem -> "  - " + problem)
			.collect(Collectors.joining(System.lineSeparator(), "Άκυρο αρχείο αποστολής " + source + ":"
					+ System.lineSeparator(), ""));
	}

}
