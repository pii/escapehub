package com.escapehub.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Το ζητούμενο αντικείμενο δεν υπάρχει. Επιστρέφεται ως 404 με {@code ProblemDetail}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String message) {
		super(message);
	}

}
