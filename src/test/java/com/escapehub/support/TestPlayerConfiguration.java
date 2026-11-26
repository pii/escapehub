package com.escapehub.support;

import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.escapehub.user.CurrentPlayerProvider;
import com.escapehub.user.PlayerId;

/**
 * Τρέχων παίκτης για τους ελέγχους, από το header {@link #HEADER}.
 *
 * <pre>{@code
 * @WebMvcTest(TeamController.class)
 * @Import(TestPlayerConfiguration.class)
 * ...
 * mvc.post().uri("/api/teams").header(TestPlayerConfiguration.HEADER, "7")
 * }</pre>
 *
 * Χωρίς το header ο παίκτης δεν αναγνωρίζεται και η απάντηση είναι 401.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestPlayerConfiguration {

	public static final String HEADER = "X-Test-Player-Id";

	@Bean
	@Primary
	CurrentPlayerProvider testCurrentPlayerProvider() {
		return request -> Optional.ofNullable(request.getHeader(HEADER))
			.map(Long::parseLong)
			.map(PlayerId::new);
	}

}
