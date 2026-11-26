package com.escapehub.mission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.escapehub.common.NotFoundException;
import com.escapehub.mission.domain.Mission;
import com.escapehub.mission.infrastructure.MissionFiles;

class MissionCatalogTests {

	private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

	private static final String LOCATION = "classpath:mission-fixtures/valid/*.yml";

	private final MissionCatalog catalog = new MissionCatalog(
			new MissionFiles(LOCATION, new DefaultResourceLoader(), VALIDATION.getValidator()));

	@AfterAll
	static void closeValidation() {
		VALIDATION.close();
	}

	@Test
	void listsTheMissionsOfTheFiles() {
		assertThat(catalog.all()).extracting(Mission::id).containsExactly("small-test");
	}

	@Test
	void findsMissionById() {
		assertThat(catalog.find("small-test")).map(Mission::title).contains("Μικρή αποστολή ελέγχων");
		assertThat(catalog.get("small-test").version()).isEqualTo(3);
	}

	@Test
	void unknownIdIsNotFound() {
		assertThat(catalog.find("no-such-mission")).isEmpty();
		assertThatThrownBy(() -> catalog.get("no-such-mission")).isInstanceOf(NotFoundException.class)
			.hasMessage(MissionCatalog.MISSION_NOT_FOUND);
	}

}
