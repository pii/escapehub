package com.escapehub.team.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class InviteCodeTests {

	@Test
	void generatedCodesHaveEightUnambiguousCharacters() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			String code = InviteCode.generate();
			assertThat(code).matches("[A-HJKMNP-Z2-9]{8}");
			codes.add(code);
		}
		assertThat(codes).hasSizeGreaterThan(195);
	}

	@Test
	void normalizesSpacesAndCase() {
		assertThat(InviteCode.normalize("  abcd2345 ")).contains("ABCD2345");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "ABC", "ABCD23456", "ABCD-234", "ABCD0123", "ABCDIL23" })
	void rejectsCodesWithWrongFormat(String raw) {
		assertThat(InviteCode.normalize(raw)).isEmpty();
	}

}
