package com.escapehub.team.application;

import com.escapehub.team.TeamRole;
import com.escapehub.user.PlayerId;

/**
 * Ένα μέλος ομάδας, με το όνομα παίκτη και τον ρόλο του.
 */
public record TeamMember(PlayerId player, String name, TeamRole role) {
}
