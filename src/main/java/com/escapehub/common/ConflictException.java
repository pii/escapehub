package com.escapehub.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Η ενέργεια δεν επιτρέπεται στην τρέχουσα κατάσταση, π.χ. λύση κλειδωμένου γρίφου ή όνομα που υπάρχει ήδη.
 * Επιστρέφεται ως 409 με {@code ProblemDetail}.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConflictException(String message) {
		super(message);
	}

}
