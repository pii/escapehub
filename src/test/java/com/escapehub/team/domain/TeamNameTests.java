package com.escapehub.team.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapehub.common.BadRequestException;

class TeamNameTests {

	@ParameterizedTest
	@ValueSource(strings = { "Οι Ερευνητές", "Team 7", "A" })
	void acceptsNonBlankNamesUpTo40Characters(String name) {
		assertThat(TeamName.of(name).value()).isEqualTo(name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "  Οι Ερευνητές ", "\tTeam 7\n" })
	void stripsLeadingAndTrailingWhitespace(String name) {
		assertThat(TeamName.of(name).value()).isEqualTo(name.strip());
	}

	@Test
	void acceptsExactly40AndRejects41Characters() {
		assertThat(TeamName.of("Ω".repeat(40)).value()).hasSize(40);
		assertThatThrownBy(() -> TeamName.of("Ω".repeat(41))).isInstanceOf(BadRequestException.class);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   ", "Αυτό το όνομα έχει παραπάνω από σαράντα χαρακτήρες" })
	void rejectsBlankOrTooLongNames(String name) {
		assertThatThrownBy(() -> TeamName.of(name))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("υποχρεωτικό");
	}

}
