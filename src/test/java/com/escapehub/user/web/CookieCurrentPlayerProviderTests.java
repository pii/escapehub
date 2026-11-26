package com.escapehub.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.escapehub.user.PlayerId;
import com.escapehub.user.application.PlayerService;

class CookieCurrentPlayerProviderTests {

	private final PlayerService players = mock(PlayerService.class);

	private final CookieCurrentPlayerProvider provider = new CookieCurrentPlayerProvider(players);

	@Test
	void recognisesPlayerFromCookie() {
		when(players.findByToken("token-1")).thenReturn(Optional.of(new PlayerId(3)));

		assertThat(provider.currentPlayer(requestWithCookie("token-1"))).contains(new PlayerId(3));
	}

	@Test
	void looksUpTheDatabaseOncePerRequest() {
		when(players.findByToken("token-1")).thenReturn(Optional.of(new PlayerId(3)));
		MockHttpServletRequest request = requestWithCookie("token-1");

		provider.currentPlayer(request);
		provider.currentPlayer(request);

		verify(players, times(1)).findByToken("token-1");
	}

	@Test
	void noCookieMeansNoPlayer() {
		assertThat(provider.currentPlayer(new MockHttpServletRequest())).isEmpty();
		verifyNoInteractions(players);
	}

	@Test
	void unknownTokenMeansNoPlayer() {
		when(players.findByToken("stale")).thenReturn(Optional.empty());

		assertThat(provider.currentPlayer(requestWithCookie("stale"))).isEmpty();
	}

	private static MockHttpServletRequest requestWithCookie(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(PlayerCookie.NAME, token));
		return request;
	}

}
