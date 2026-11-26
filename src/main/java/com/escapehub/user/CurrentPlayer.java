package com.escapehub.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Σημαίνει μια παράμετρο τύπου {@link PlayerId} σε controller, που παίρνει τον παίκτη του αιτήματος.
 *
 * <pre>{@code
 * @PostMapping("/api/teams")
 * TeamResponse create(@CurrentPlayer PlayerId player, @Valid @RequestBody CreateTeamRequest request)
 * }</pre>
 *
 * Αν το αίτημα δεν έχει αναγνωρισμένο παίκτη, η απάντηση είναι 401.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentPlayer {
}
