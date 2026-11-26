package com.escapehub.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ο παίκτης είναι γνωστός, αλλά δεν έχει δικαίωμα για την ενέργεια, π.χ. απλό μέλος που ξεκινά αποστολή ή παίκτης
 * που ζητά playthrough άλλης ομάδας. Επιστρέφεται ως 403 με {@code ProblemDetail}.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ForbiddenException(String message) {
		super(message);
	}

}
