package com.escapehub.team.application;

import com.escapehub.team.TeamId;
import com.escapehub.team.TeamRole;

/**
 * Μια ομάδα στη λίστα «Οι ομάδες μου», με τον ρόλο του παίκτη σε αυτήν.
 */
public record TeamSummary(TeamId id, String name, TeamRole role) {
}
