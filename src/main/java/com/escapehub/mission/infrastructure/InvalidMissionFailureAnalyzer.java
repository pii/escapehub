package com.escapehub.mission.infrastructure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Όταν ένα αρχείο αποστολής είναι άκυρο, η εκκίνηση δείχνει μόνο το αρχείο και τα πεδία με πρόβλημα, αντί για
 * ολόκληρο το stack trace. Καταχωρίζεται στο {@code META-INF/spring.factories}.
 */
class InvalidMissionFailureAnalyzer extends AbstractFailureAnalyzer<InvalidMissionException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidMissionException cause) {
		String action = "Διορθώστε το αρχείο της αποστολής. Η μορφή του: docs/mission-format.md";
		return new FailureAnalysis(cause.getMessage(), action, cause);
	}

}
