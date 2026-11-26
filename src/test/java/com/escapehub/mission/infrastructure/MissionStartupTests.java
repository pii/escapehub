package com.escapehub.mission.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import com.escapehub.mission.application.MissionCatalog;

/**
 * Οι αποστολές φορτώνονται στην εκκίνηση, και ένα άκυρο αρχείο τη σταματά με μήνυμα που δείχνει το αρχείο και το
 * πεδίο (#26, AC1 και AC2). Χωρίς βάση δεδομένων: μόνο τα beans του module.
 */
class MissionStartupTests {

	private static final String LOCATION = "escapehub.missions.location=classpath:mission-fixtures/invalid/";

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
		.withUserConfiguration(MissionFiles.class, MissionCatalog.class);

	@Test
	void loadsTheMissionFilesOnStartup() {
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(MissionCatalog.class).find("villa-diodati")).isPresent();
		});
	}

	@Test
	void invalidMissionFileStopsStartupNamingFileAndField() {
		runner.withPropertyValues(LOCATION + "unknown-evidence.yml").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).rootCause()
				.isInstanceOf(InvalidMissionException.class)
				.hasMessageContaining("unknown-evidence.yml")
				.hasMessageContaining("puzzles[1].unlockedBy[1]: άγνωστο στοιχείο 'missing-clue'");
		});
	}

	@Test
	void failureAnalysisShowsOnlyTheMissionProblems() {
		runner.withPropertyValues(LOCATION + "unlock-cycle.yml").run(context -> {
			Throwable failure = context.getStartupFailure();
			FailureAnalysis analysis = new InvalidMissionFailureAnalyzer().analyze(failure);

			assertThat(analysis).isNotNull();
			assertThat(analysis.getDescription()).startsWith("Άκυρο αρχείο αποστολής")
				.contains("unlock-cycle.yml")
				.contains("puzzles[0].unlockedBy: κύκλος");
			assertThat(analysis.getAction()).contains("docs/mission-format.md");
		});
	}

}
