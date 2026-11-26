package com.escapehub.team.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapehub.common.NotFoundException;
import com.escapehub.team.TeamId;
import com.escapehub.team.application.TeamDetails;
import com.escapehub.team.application.TeamMember;
import com.escapehub.team.application.TeamService;
import com.escapehub.team.application.TeamSummary;
import com.escapehub.user.CurrentPlayer;
import com.escapehub.user.PlayerId;

/**
 * REST API για τις ομάδες παικτών (US-03, US-04). Τα σφάλματα επιστρέφονται ως {@code ProblemDetail}.
 */
@RestController
@RequestMapping("/api/teams")
class TeamApiController {

	private final TeamService teams;

	TeamApiController(TeamService teams) {
		this.teams = teams;
	}

	/**
	 * Δημιουργεί ομάδα με τον τρέχοντα παίκτη ως δημιουργό. Απαντά 201, ή 400 για κενό όνομα.
	 */
	@PostMapping
	ResponseEntity<TeamResponse> create(@CurrentPlayer PlayerId player,
			@Valid @RequestBody CreateTeamRequest body) {
		TeamDetails team = teams.create(player, body.name());
		URI location = URI.create("/api/teams/" + team.id().value());
		return ResponseEntity.created(location).body(TeamResponse.of(team));
	}

	/**
	 * Συμμετοχή σε ομάδα με κωδικό πρόσκλησης. Απαντά 200 και όταν ο παίκτης ήταν ήδη μέλος, ή 404 για άκυρο
	 * κωδικό.
	 */
	@PostMapping("/join")
	TeamResponse join(@CurrentPlayer PlayerId player, @Valid @RequestBody JoinTeamRequest body) {
		return TeamResponse.of(teams.join(player, body.inviteCode()).team());
	}

	/**
	 * Οι ομάδες του τρέχοντος παίκτη.
	 */
	@GetMapping
	List<TeamSummaryResponse> mine(@CurrentPlayer PlayerId player) {
		return teams.teamsOf(player).stream().map(TeamSummaryResponse::of).toList();
	}

	/**
	 * Μια ομάδα με τα μέλη της. Απαντά 404 αν η ομάδα δεν υπάρχει ή ο παίκτης δεν είναι μέλος της.
	 */
	@GetMapping("/{id}")
	TeamResponse team(@CurrentPlayer PlayerId player, @PathVariable long id) {
		if (id <= 0) {
			throw new NotFoundException("Η ομάδα δεν βρέθηκε.");
		}
		return TeamResponse.of(teams.details(new TeamId(id), player));
	}

	record CreateTeamRequest(@NotNull String name) {
	}

	record JoinTeamRequest(@NotNull String inviteCode) {
	}

	record TeamSummaryResponse(long id, String name, String role) {

		static TeamSummaryResponse of(TeamSummary team) {
			return new TeamSummaryResponse(team.id().value(), team.name(), team.role().name());
		}

	}

	record TeamResponse(long id, String name, String inviteCode, List<MemberResponse> members) {

		static TeamResponse of(TeamDetails team) {
			List<MemberResponse> members = team.members()
				.stream()
				.map(MemberResponse::of)
				.toList();
			return new TeamResponse(team.id().value(), team.name(), team.inviteCode(), members);
		}

	}

	record MemberResponse(long playerId, String name, String role) {

		static MemberResponse of(TeamMember member) {
			return new MemberResponse(member.player().value(), member.name(), member.role().name());
		}

	}

}
