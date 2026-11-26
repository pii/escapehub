package com.escapehub.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapehub.common.BadRequestException;

class PlayerNameTests {

	@ParameterizedTest
	@ValueSource(strings = { "Μαρία", "maria", "maria_99", "Νίκος-2", "abc", "abcdefghijklmnopqrst" })
	void acceptsLettersDigitsUnderscoreAndDash(String name) {
		assertThat(PlayerName.of(name).value()).isEqualTo(name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "  Νίκος ", "\tNikos\n" })
	void stripsLeadingAndTrailingWhitespace(String name) {
		assertThat(PlayerName.of(name).value()).isEqualTo(name.strip());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   ", "ab", "abcdefghijklmnopqrstu", "δύο λέξεις", "maria!", "a.b.c" })
	void rejectsInvalidNames(String name) {
		assertThatThrownBy(() -> PlayerName.of(name))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("3–20 χαρακτήρες");
	}

}
