package com.escapehub.common;

/**
 * Το ζητούμενο αντικείμενο δεν υπάρχει. Επιστρέφεται ως 404 με {@code ProblemDetail}.
 */
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String message) {
		super(message);
	}

}
