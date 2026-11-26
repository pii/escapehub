package com.escapehub.common;

/**
 * Το αίτημα χρειάζεται παίκτη, αλλά δεν έχει αναγνωριστεί κανείς. Επιστρέφεται ως 401 με {@code ProblemDetail}.
 */
public class UnauthenticatedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnauthenticatedException(String message) {
		super(message);
	}

}
