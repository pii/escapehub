package com.escapehub.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Το αίτημα χρειάζεται παίκτη, αλλά δεν έχει αναγνωριστεί κανείς. Επιστρέφεται ως 401 με {@code ProblemDetail}.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthenticatedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnauthenticatedException(String message) {
		super(message);
	}

}
