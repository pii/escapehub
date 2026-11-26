package com.escapehub.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PlayerIdTests {

	@Test
	void keepsPositiveValue() {
		assertThat(new PlayerId(5).value()).isEqualTo(5);
	}

	@Test
	void rejectsZeroAndNegative() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PlayerId(0));
		assertThatIllegalArgumentException().isThrownBy(() -> new PlayerId(-1));
	}

}
