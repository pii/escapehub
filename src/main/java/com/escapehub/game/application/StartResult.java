package com.escapehub.game.application;

/**
 * Το αποτέλεσμα της έναρξης αποστολής.
 *
 * @param playthrough το ενεργό playthrough της ομάδας
 * @param created {@code true} αν ξεκίνησε τώρα, {@code false} αν η ομάδα είχε ήδη ενεργό playthrough της ίδιας
 * αποστολής και συνεχίζει αυτό (US-06, AC3)
 */
public record StartResult(PlaythroughDetails playthrough, boolean created) {
}
