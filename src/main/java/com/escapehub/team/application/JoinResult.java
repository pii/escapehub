package com.escapehub.team.application;

/**
 * Αποτέλεσμα της συμμετοχής σε ομάδα.
 *
 * @param team η ομάδα
 * @param alreadyMember {@code true} αν ο παίκτης ήταν ήδη μέλος και δεν προστέθηκε ξανά
 */
public record JoinResult(TeamDetails team, boolean alreadyMember) {
}
