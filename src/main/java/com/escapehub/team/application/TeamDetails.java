package com.escapehub.team.application;

import java.util.List;

import com.escapehub.team.TeamId;

/**
 * Μια ομάδα όπως τη βλέπουν τα μέλη της: όνομα, κωδικός πρόσκλησης και μέλη (πρώτος ο δημιουργός).
 */
public record TeamDetails(TeamId id, String name, String inviteCode, List<TeamMember> members) {

	public TeamDetails {
		members = List.copyOf(members);
	}

}
