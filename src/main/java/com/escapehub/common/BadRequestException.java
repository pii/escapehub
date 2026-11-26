package com.escapehub.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Τα δεδομένα του αιτήματος δεν είναι έγκυρα, π.χ. όνομα παίκτη με μη επιτρεπτούς χαρακτήρες.
 * Στο API επιστρέφεται ως 400 με {@code ProblemDetail}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BadRequestException(String message) {
		super(message);
	}

}
