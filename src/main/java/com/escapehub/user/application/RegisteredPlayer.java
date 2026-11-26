package com.escapehub.user.application;

import com.escapehub.user.PlayerId;

/**
 * Αποτέλεσμα της εγγραφής ενός παίκτη.
 *
 * @param id το αναγνωριστικό του νέου παίκτη
 * @param name το όνομά του, όπως αποθηκεύτηκε
 * @param token το token για το cookie. Δεν αποθηκεύεται πουθενά αλλού, μόνο το hash του.
 */
public record RegisteredPlayer(PlayerId id, String name, String token) {
}
