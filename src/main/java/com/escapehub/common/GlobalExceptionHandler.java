package com.escapehub.common;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Ενιαίος χειρισμός σφαλμάτων για το REST API. Κάθε σφάλμα επιστρέφεται ως {@link ProblemDetail} (RFC 9457).
 *
 * <p>Ισχύει μόνο για {@code @RestController}. Οι σελίδες του UI ({@code @Controller}) δεν παίρνουν ποτέ JSON:
 * οι εξαιρέσεις τους δίνουν τη σελίδα σφάλματος του {@code templates/error/}, με τον κωδικό από το
 * {@code @ResponseStatus} της εξαίρεσης.
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Δεν βρέθηκε", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Η ενέργεια δεν επιτρέπεται", ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail handleBadRequest(BadRequestException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Μη έγκυρα δεδομένα", ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	ProblemDetail handleForbidden(ForbiddenException ex) {
		return problem(HttpStatus.FORBIDDEN, "Δεν επιτρέπεται", ex.getMessage());
	}

	@ExceptionHandler(UnauthenticatedException.class)
	ProblemDetail handleUnauthenticated(UnauthenticatedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Δεν έχει οριστεί παίκτης", ex.getMessage());
	}

	/**
	 * Αποτυχία του Bean Validation: 400 με τα λάθη ανά πεδίο στην ιδιότητα {@code errors}.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new TreeMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Μη έγκυρα δεδομένα",
				"Ένα ή περισσότερα πεδία δεν είναι έγκυρα.");
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
