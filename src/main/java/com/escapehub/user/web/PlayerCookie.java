package com.escapehub.user.web;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseCookie;

/**
 * Το cookie που αναγνωρίζει τον παίκτη: {@code HttpOnly}, {@code SameSite=Lax}, διάρκεια 30 ημέρες.
 */
final class PlayerCookie {

	static final String NAME = "ESCAPEHUB_PLAYER";

	static final Duration MAX_AGE = Duration.ofDays(30);

	private PlayerCookie() {
	}

	static String create(String token, HttpServletRequest request) {
		return ResponseCookie.from(NAME, token)
			.httpOnly(true)
			.secure(request.isSecure())
			.sameSite("Lax")
			.path("/")
			.maxAge(MAX_AGE)
			.build()
			.toString();
	}

	static Optional<String> read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> NAME.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst();
	}

}
